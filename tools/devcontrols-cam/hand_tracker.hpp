#pragma once
#include <opencv2/opencv.hpp>
#include <onnxruntime_cxx_api.h>
#include <vector>
#include <string>
#include <memory>

struct HandData {
    bool detected = false;
    float confidence = 0.0f;
    cv::Rect2f bbox;
    std::vector<cv::Point3f> landmarks; // 21 3D landmarks in image pixels
    cv::Point2f palm_center;
    bool is_right_hand = true;
};

class HandTracker {
private:
    Ort::Env env;
    std::unique_ptr<Ort::Session> palm_session;
    std::unique_ptr<Ort::Session> hand_session;
    Ort::MemoryInfo memory_info;

    std::vector<cv::Point2f> anchors;
    std::string palm_in_name;
    std::string palm_out0_name;
    std::string palm_out1_name;

    std::string hand_in_name;
    std::string hand_out0_name; // landmarks
    std::string hand_out1_name; // conf
    std::string hand_out2_name; // handedness

public:
    HandTracker(const std::string& palm_model_path, const std::string& hand_model_path);
    HandData process(const cv::Mat& bgr_frame);
};
