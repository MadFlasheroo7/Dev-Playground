#include <onnxruntime_cxx_api.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <vector>
#include <cmath>

// Forward declarations & anchor definitions
extern std::vector<cv::Point2f> get_anchors();

int main() {
    std::cout << "Testing full ONNX Runtime Palm + HandPose pipeline...\n";
    try {
        Ort::Env env(ORT_LOGGING_LEVEL_WARNING, "HandPipeline");
        Ort::SessionOptions session_options;
        session_options.SetIntraOpNumThreads(2);

        Ort::Session palm_session(env, "models/palm_detection.onnx", session_options);
        Ort::Session hand_session(env, "models/handpose_estimation.onnx", session_options);
        std::cout << "Both models initialized successfully in ONNX Runtime!\n";
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << "\n";
        return 1;
    }
    return 0;
}
