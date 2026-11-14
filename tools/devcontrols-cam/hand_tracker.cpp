#include <opencv2/imgproc.hpp>
#include <opencv2/geometry/2d.hpp>
#include "hand_tracker.hpp"
#include "palm_anchors.hpp"
#include <cmath>
#include <iostream>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

HandTracker::HandTracker(const std::string& palm_model_path, const std::string& hand_model_path)
    : env(ORT_LOGGING_LEVEL_WARNING, "HandTracker"),
      memory_info(Ort::MemoryInfo::CreateCpu(OrtArenaAllocator, OrtMemTypeDefault)) {
    
    Ort::SessionOptions opts;
    opts.SetIntraOpNumThreads(2);
    opts.SetGraphOptimizationLevel(GraphOptimizationLevel::ORT_ENABLE_ALL);

    palm_session = std::make_unique<Ort::Session>(env, palm_model_path.c_str(), opts);
    hand_session = std::make_unique<Ort::Session>(env, hand_model_path.c_str(), opts);

    Ort::AllocatorWithDefaultOptions alloc;
    palm_in_name = palm_session->GetInputNameAllocated(0, alloc).get();
    palm_out0_name = palm_session->GetOutputNameAllocated(0, alloc).get();
    palm_out1_name = palm_session->GetOutputNameAllocated(1, alloc).get();

    hand_in_name = hand_session->GetInputNameAllocated(0, alloc).get();
    hand_out0_name = hand_session->GetOutputNameAllocated(0, alloc).get();
    hand_out1_name = hand_session->GetOutputNameAllocated(1, alloc).get();
    hand_out2_name = hand_session->GetOutputNameAllocated(2, alloc).get();

    anchors = load_palm_anchors();
}

HandData HandTracker::process(const cv::Mat& bgr_frame) {
    HandData result;
    if (bgr_frame.empty()) return result;

    const cv::Size palm_input_size(192, 192);
    float ratio = std::min(192.0f / bgr_frame.cols, 192.0f / bgr_frame.rows);
    cv::Size ratio_size(static_cast<int>(bgr_frame.cols * ratio), static_cast<int>(bgr_frame.rows * ratio));

    cv::Mat resized;
    cv::resize(bgr_frame, resized, ratio_size);

    int pad_w = 192 - ratio_size.width;
    int pad_h = 192 - ratio_size.height;
    int pad_left = pad_w / 2;
    int pad_top = pad_h / 2;

    cv::Mat padded;
    cv::copyMakeBorder(resized, padded, pad_top, pad_h - pad_top, pad_left, pad_w - pad_left,
                       cv::BORDER_CONSTANT, cv::Scalar(0, 0, 0));

    cv::Mat rgb;
    cv::cvtColor(padded, rgb, cv::COLOR_BGR2RGB);
    rgb.convertTo(rgb, CV_32FC3, 1.0 / 255.0);

    // 1. Palm Detection forward pass
    std::vector<int64_t> palm_shape = {1, 192, 192, 3};
    Ort::Value palm_tensor = Ort::Value::CreateTensor<float>(
        memory_info, (float*)rgb.data, 192 * 192 * 3, palm_shape.data(), palm_shape.size());

    const char* p_in[] = {palm_in_name.c_str()};
    const char* p_out[] = {palm_out0_name.c_str(), palm_out1_name.c_str()};
    auto palm_outputs = palm_session->Run(Ort::RunOptions{nullptr}, p_in, &palm_tensor, 1, p_out, 2);

    float* boxes_lms = palm_outputs[0].GetTensorMutableData<float>(); // [1, 2014, 18]
    float* scores    = palm_outputs[1].GetTensorMutableData<float>(); // [1, 2014, 1]

    float best_score = 0.50f;
    int best_idx = -1;
    for (int i = 0; i < 2014; ++i) {
        float s = 1.0f / (1.0f + std::exp(-scores[i]));
        if (s > best_score) {
            best_score = s;
            best_idx = i;
        }
    }

    if (best_idx < 0) {
        return result; // No palm detected
    }

    // Unproject best palm detection
    float scale = std::max(bgr_frame.rows, bgr_frame.cols);
    float pad_bias_x = pad_left / ratio;
    float pad_bias_y = pad_top / ratio;

    float cxy_x = boxes_lms[best_idx * 18 + 0] / 192.0f;
    float cxy_y = boxes_lms[best_idx * 18 + 1] / 192.0f;
    float wh_x  = boxes_lms[best_idx * 18 + 2] / 192.0f;
    float wh_y  = boxes_lms[best_idx * 18 + 3] / 192.0f;

    cv::Point2f anchor = anchors[best_idx];
    cv::Point2f xy1((cxy_x - wh_x / 2 + anchor.x) * scale - pad_bias_x,
                    (cxy_y - wh_y / 2 + anchor.y) * scale - pad_bias_y);
    cv::Point2f xy2((cxy_x + wh_x / 2 + anchor.x) * scale - pad_bias_x,
                    (cxy_y + wh_y / 2 + anchor.y) * scale - pad_bias_y);

    std::vector<cv::Point2f> palm_lms;
    for (int j = 0; j < 7; ++j) {
        float dx = (boxes_lms[best_idx * 18 + 4 + j * 2 + 0] / 192.0f + anchor.x) * scale - pad_bias_x;
        float dy = (boxes_lms[best_idx * 18 + 4 + j * 2 + 1] / 192.0f + anchor.y) * scale - pad_bias_y;
        palm_lms.push_back(cv::Point2f(dx, dy));
    }

    cv::Point2f p1 = palm_lms[0]; // palm base / wrist
    cv::Point2f p2 = palm_lms[2]; // middle finger base

    // Calculate rotation angle to align hand upright
    double radians = M_PI / 2.0 - std::atan2(-(p2.y - p1.y), p2.x - p1.x);
    while (radians > M_PI) radians -= 2.0 * M_PI;
    while (radians < -M_PI) radians += 2.0 * M_PI;
    double angle_deg = radians * 180.0 / M_PI;

    // Expand bounding box for hand pose
    cv::Point2f palm_center = (xy1 + xy2) * 0.5f;
    float palm_w = xy2.x - xy1.x;
    float palm_h = xy2.y - xy1.y;
    float hand_box_size = std::max(palm_w, palm_h) * 2.8f;

    // Hand crop matrix
    cv::Mat rot_mat = cv::getRotationMatrix2D(palm_center, angle_deg, 1.0);
    rot_mat.at<double>(0, 2) += (hand_box_size / 2.0 - palm_center.x);
    rot_mat.at<double>(1, 2) += (hand_box_size / 2.0 - palm_center.y);

    cv::Mat hand_crop;
    cv::warpAffine(bgr_frame, hand_crop, rot_mat, cv::Size(hand_box_size, hand_box_size),
                   cv::INTER_LINEAR, cv::BORDER_CONSTANT, cv::Scalar(0, 0, 0));

    cv::Mat hand_resized;
    cv::resize(hand_crop, hand_resized, cv::Size(224, 224));
    cv::Mat hand_rgb;
    cv::cvtColor(hand_resized, hand_rgb, cv::COLOR_BGR2RGB);
    hand_rgb.convertTo(hand_rgb, CV_32FC3, 1.0 / 255.0);

    // 2. Hand Pose Estimation forward pass
    std::vector<int64_t> hand_shape = {1, 224, 224, 3};
    Ort::Value hand_tensor = Ort::Value::CreateTensor<float>(
        memory_info, (float*)hand_rgb.data, 224 * 224 * 3, hand_shape.data(), hand_shape.size());

    const char* h_in[] = {hand_in_name.c_str()};
    const char* h_out[] = {hand_out0_name.c_str(), hand_out1_name.c_str(), hand_out2_name.c_str()};
    auto hand_outputs = hand_session->Run(Ort::RunOptions{nullptr}, h_in, &hand_tensor, 1, h_out, 3);

    float* raw_lms = hand_outputs[0].GetTensorMutableData<float>(); // [1, 63]
    float* h_conf  = hand_outputs[1].GetTensorMutableData<float>(); // [1, 1]
    float* h_hand  = hand_outputs[2].GetTensorMutableData<float>(); // [1, 1]

    float hand_confidence = h_conf[0];
    if (hand_confidence < 0.5f) {
        return result;
    }

    // Invert rotation to map landmarks back to original frame
    cv::Mat inv_rot_mat;
    cv::invertAffineTransform(rot_mat, inv_rot_mat);

    result.detected = true;
    result.confidence = hand_confidence;
    result.is_right_hand = (h_hand[0] > 0.5f);
    result.palm_center = palm_center;
    result.bbox = cv::Rect2f(xy1.x, xy1.y, xy2.x - xy1.x, xy2.y - xy1.y);

    result.landmarks.resize(21);
    for (int i = 0; i < 21; ++i) {
        float lx_crop = (raw_lms[i * 3 + 0] / 224.0f) * hand_box_size;
        float ly_crop = (raw_lms[i * 3 + 1] / 224.0f) * hand_box_size;
        float lz      = (raw_lms[i * 3 + 2] / 224.0f) * hand_box_size;

        // Apply inverse affine transform
        double orig_x = inv_rot_mat.at<double>(0, 0) * lx_crop +
                        inv_rot_mat.at<double>(0, 1) * ly_crop +
                        inv_rot_mat.at<double>(0, 2);
        double orig_y = inv_rot_mat.at<double>(1, 0) * lx_crop +
                        inv_rot_mat.at<double>(1, 1) * ly_crop +
                        inv_rot_mat.at<double>(1, 2);

        result.landmarks[i] = cv::Point3f(static_cast<float>(orig_x),
                                          static_cast<float>(orig_y),
                                          lz);
    }

    return result;
}
