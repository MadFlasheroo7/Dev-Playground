#include <opencv2/opencv.hpp>
#include <iostream>
#include <chrono>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <linux/videodev2.h>
#include <cstring>
#include <vector>

// ── Control State (mapped to UI Trackbars) ──────────────────────────────────
int g_sharpness     = 35;  // 0 - 100 (Unsharp Mask)
int g_clahe         = 40;  // 0 - 100 (Backlight / Shadow Lift)
int g_denoise       = 15;  // 0 - 100 (Bilateral Denoise)
int g_warmth        = 50;  // 0 - 100 (50 is neutral, >50 warm, <50 cool)
int g_contrast      = 50;  // 0 - 100 (50 is 1.0x neutral)
int g_brightness    = 50;  // 0 - 100 (50 is 0 neutral)
int g_compare_mode  = 0;   // 0 = Full Enhanced, 1 = Side-by-Side (Raw vs Enhanced)
int g_vcam_active   = 0;   // 0 = Off, 1 = Stream to /dev/video20

// V4L2 Virtual Camera Output File Descriptor
int g_vcam_fd = -1;

void init_vcam(int width, int height) {
    if (g_vcam_fd >= 0) {
        close(g_vcam_fd);
        g_vcam_fd = -1;
    }
    g_vcam_fd = open("/dev/video20", O_RDWR);
    if (g_vcam_fd < 0) {
        std::cerr << "[VirtualCam] /dev/video20 not available. Run: sudo modprobe v4l2loopback devices=1 card_label=\"DevControls Enhanced Cam\" exclusive_caps=1\n";
        return;
    }

    struct v4l2_format vid_format;
    memset(&vid_format, 0, sizeof(vid_format));
    vid_format.type = V4L2_BUF_TYPE_VIDEO_OUTPUT;
    vid_format.fmt.pix.width = width;
    vid_format.fmt.pix.height = height;
    vid_format.fmt.pix.pixelformat = V4L2_PIX_FMT_YUYV;
    vid_format.fmt.pix.sizeimage = width * height * 2;
    vid_format.fmt.pix.field = V4L2_FIELD_NONE;

    if (ioctl(g_vcam_fd, VIDIOC_S_FMT, &vid_format) < 0) {
        std::cerr << "[VirtualCam] Failed to set VIDIOC_S_FMT on /dev/video20\n";
        close(g_vcam_fd);
        g_vcam_fd = -1;
        return;
    }
    std::cout << "[VirtualCam] Successfully opened /dev/video20 (1280x720 YUYV)\n";
}

void write_vcam_frame(const cv::Mat& bgr_frame) {
    if (g_vcam_fd < 0) return;
    cv::Mat yuyv;
    cv::cvtColor(bgr_frame, yuyv, cv::COLOR_BGR2YUV_YUYV);
    ssize_t written = write(g_vcam_fd, yuyv.data, yuyv.total() * yuyv.elemSize());
    (void)written;
}

void apply_preset(int preset_id) {
    switch (preset_id) {
        case 1: // Default / Balanced
            g_sharpness = 30; g_clahe = 30; g_denoise = 10; g_warmth = 50; g_contrast = 50; g_brightness = 50;
            break;
        case 2: // Pro Studio (Crisp details + warm natural tones)
            g_sharpness = 60; g_clahe = 40; g_denoise = 15; g_warmth = 58; g_contrast = 56; g_brightness = 52;
            break;
        case 3: // Backlight Fix (High CLAHE for bright windows behind desk)
            g_sharpness = 30; g_clahe = 85; g_denoise = 15; g_warmth = 53; g_contrast = 60; g_brightness = 55;
            break;
        case 4: // Low-Light Denoise (Smooths camera sensor grain in dim room)
            g_sharpness = 20; g_clahe = 50; g_denoise = 60; g_warmth = 52; g_contrast = 52; g_brightness = 60;
            break;
    }
    cv::setTrackbarPos("Sharpness", "Controls", g_sharpness);
    cv::setTrackbarPos("Backlight / CLAHE", "Controls", g_clahe);
    cv::setTrackbarPos("Denoise", "Controls", g_denoise);
    cv::setTrackbarPos("Warmth", "Controls", g_warmth);
    cv::setTrackbarPos("Contrast", "Controls", g_contrast);
    cv::setTrackbarPos("Brightness", "Controls", g_brightness);
}

int main() {
    std::cout << "========================================================\n";
    std::cout << "  DevControls Native Linux Camera Engine (C++ / OpenCV 5)\n";
    std::cout << "========================================================\n";

    // Open physical webcam via direct Linux V4L2 capture
    cv::VideoCapture cap(0, cv::CAP_V4L2);
    if (!cap.isOpened()) {
        std::cerr << "ERROR: Failed to open /dev/video0. Make sure your camera is connected.\n";
        return 1;
    }

    // Configure hardware capture parameters for ASUS ROG EYE S
    cap.set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('M', 'J', 'P', 'G'));
    cap.set(cv::CAP_PROP_FRAME_WIDTH, 1280);
    cap.set(cv::CAP_PROP_FRAME_HEIGHT, 720);
    cap.set(cv::CAP_PROP_FPS, 60);

    const int width = 1280;
    const int height = 720;

    std::cout << "[Hardware] Connected to camera at " << width << "x" << height << " @ 60 FPS (MJPEG)\n";

    // Setup UI Windows
    const std::string win_stream = "DevControls Native Camera Stream";
    const std::string win_ctrl = "Controls";

    cv::namedWindow(win_stream, cv::WINDOW_NORMAL);
    cv::namedWindow(win_ctrl, cv::WINDOW_AUTOSIZE);

    // Create Interactive Sliders
    cv::createTrackbar("Sharpness", win_ctrl, &g_sharpness, 100);
    cv::createTrackbar("Backlight / CLAHE", win_ctrl, &g_clahe, 100);
    cv::createTrackbar("Denoise", win_ctrl, &g_denoise, 100);
    cv::createTrackbar("Warmth", win_ctrl, &g_warmth, 100);
    cv::createTrackbar("Contrast", win_ctrl, &g_contrast, 100);
    cv::createTrackbar("Brightness", win_ctrl, &g_brightness, 100);
    cv::createTrackbar("Compare (0/1)", win_ctrl, &g_compare_mode, 1);
    cv::createTrackbar("VirtualCam (0/1)", win_ctrl, &g_vcam_active, 1);

    init_vcam(width, height);

    cv::Ptr<cv::CLAHE> clahe = cv::createCLAHE(2.0, cv::Size(8, 8));

    cv::Mat raw_frame, processed_frame, display_frame;
    cv::Mat lab_img, blurred_img;
    std::vector<cv::Mat> lab_planes(3);

    auto last_fps_time = std::chrono::high_resolution_clock::now();
    int frame_counter = 0;
    double measured_fps = 60.0;

    std::cout << "\n[Hotkeys]\n";
    std::cout << "  [C] Toggle Compare Mode (Side-by-side)\n";
    std::cout << "  [V] Toggle /dev/video20 Virtual Camera\n";
    std::cout << "  [1] Preset: Default\n";
    std::cout << "  [2] Preset: Pro Studio\n";
    std::cout << "  [3] Preset: Backlight Fix\n";
    std::cout << "  [4] Preset: Low-Light Denoise\n";
    std::cout << "  [ESC/Q] Exit\n\n";

    while (true) {
        cap >> raw_frame;
        if (raw_frame.empty()) {
            std::cerr << "Warning: Empty frame received from camera\n";
            continue;
        }

        frame_counter++;
        auto now = std::chrono::high_resolution_clock::now();
        double elapsed_sec = std::chrono::duration<double>(now - last_fps_time).count();
        if (elapsed_sec >= 0.5) {
            measured_fps = frame_counter / elapsed_sec;
            frame_counter = 0;
            last_fps_time = now;
        }

        processed_frame = raw_frame.clone();

        // 1. Denoise (Fast Gaussian or Bilateral smoothing)
        if (g_denoise > 5) {
            int ksize = (g_denoise / 20) * 2 + 1;
            ksize = std::max(3, std::min(ksize, 7));
            cv::GaussianBlur(processed_frame, processed_frame, cv::Size(ksize, ksize), 0);
        }

        // 2. CLAHE Backlight Correction in LAB Color Space
        if (g_clahe > 5) {
            double clip_limit = 1.0 + (g_clahe / 100.0) * 3.5;
            clahe->setClipLimit(clip_limit);

            cv::cvtColor(processed_frame, lab_img, cv::COLOR_BGR2Lab);
            cv::split(lab_img, lab_planes);
            clahe->apply(lab_planes[0], lab_planes[0]);
            cv::merge(lab_planes, lab_img);
            cv::cvtColor(lab_img, processed_frame, cv::COLOR_Lab2BGR);
        }

        // 3. Contrast & Brightness Adjustment
        double alpha = g_contrast / 50.0; // 0.0 to 2.0 (1.0 neutral)
        double beta  = (g_brightness - 50) * 1.5; // -75 to +75
        if (alpha != 1.0 || beta != 0.0) {
            processed_frame.convertTo(processed_frame, -1, alpha, beta);
        }

        // 4. Color Temperature (Warm / Cool Tint)
        if (g_warmth != 50) {
            int shift = (g_warmth - 50); // -50 (Cool) to +50 (Warm)
            std::vector<cv::Mat> channels(3);
            cv::split(processed_frame, channels);
            if (shift > 0) {
                // Warmer: increase Red, decrease Blue
                channels[2] += shift;     // R
                channels[0] -= shift / 2; // B
            } else {
                // Cooler: increase Blue, decrease Red
                channels[0] += (-shift);     // B
                channels[2] -= (-shift) / 2; // R
            }
            cv::merge(channels, processed_frame);
        }

        // 5. Unsharp Mask Detail Enhancer
        if (g_sharpness > 5) {
            double amount = (g_sharpness / 100.0) * 1.8;
            cv::GaussianBlur(processed_frame, blurred_img, cv::Size(0, 0), 2.5);
            cv::addWeighted(processed_frame, 1.0 + amount, blurred_img, -amount, 0, processed_frame);
        }

        // 6. Write to Virtual Camera (/dev/video20) if enabled
        if (g_vcam_active && g_vcam_fd >= 0) {
            write_vcam_frame(processed_frame);
        }

        // 7. Render Stream Preview
        if (g_compare_mode) {
            // Side-by-side comparison: Left=Raw, Right=Enhanced
            cv::Mat raw_half, proc_half;
            cv::resize(raw_frame, raw_half, cv::Size(width / 2, height));
            cv::resize(processed_frame, proc_half, cv::Size(width / 2, height));

            cv::hconcat(raw_half, proc_half, display_frame);

            // Dividing line and Labels
            cv::line(display_frame, cv::Point(width / 2, 0), cv::Point(width / 2, height), cv::Scalar(0, 255, 200), 2);
            cv::putText(display_frame, "RAW FEED", cv::Point(20, 40), cv::FONT_HERSHEY_DUPLEX, 0.8, cv::Scalar(0, 0, 0), 4);
            cv::putText(display_frame, "RAW FEED", cv::Point(20, 40), cv::FONT_HERSHEY_DUPLEX, 0.8, cv::Scalar(255, 255, 255), 2);

            cv::putText(display_frame, "ENHANCED", cv::Point(width / 2 + 20, 40), cv::FONT_HERSHEY_DUPLEX, 0.8, cv::Scalar(0, 0, 0), 4);
            cv::putText(display_frame, "ENHANCED", cv::Point(width / 2 + 20, 40), cv::FONT_HERSHEY_DUPLEX, 0.8, cv::Scalar(0, 255, 120), 2);
        } else {
            display_frame = processed_frame.clone();
        }

        // HUD Overlay on Preview Window
        char hud_buf[128];
        snprintf(hud_buf, sizeof(hud_buf), "%.1f FPS | 1280x720", measured_fps);
        cv::putText(display_frame, hud_buf, cv::Point(20, height - 20), cv::FONT_HERSHEY_DUPLEX, 0.7, cv::Scalar(0, 0, 0), 3);
        cv::putText(display_frame, hud_buf, cv::Point(20, height - 20), cv::FONT_HERSHEY_DUPLEX, 0.7, cv::Scalar(0, 255, 200), 1);

        if (g_vcam_active) {
            const std::string vcam_badge = "LIVE ON /dev/video20 (Google Meet)";
            cv::putText(display_frame, vcam_badge, cv::Point(width - 450, height - 20), cv::FONT_HERSHEY_DUPLEX, 0.7, cv::Scalar(0, 0, 0), 3);
            cv::putText(display_frame, vcam_badge, cv::Point(width - 450, height - 20), cv::FONT_HERSHEY_DUPLEX, 0.7, cv::Scalar(0, 255, 120), 1);
        }

        cv::imshow(win_stream, display_frame);

        // Handle Keyboard Shortcuts
        int key = cv::waitKey(1) & 0xFF;
        if (key == 27 || key == 'q' || key == 'Q') {
            break;
        } else if (key == 'c' || key == 'C') {
            g_compare_mode = !g_compare_mode;
            cv::setTrackbarPos("Compare (0/1)", win_ctrl, g_compare_mode);
        } else if (key == 'v' || key == 'V') {
            g_vcam_active = !g_vcam_active;
            cv::setTrackbarPos("VirtualCam (0/1)", win_ctrl, g_vcam_active);
        } else if (key >= '1' && key <= '4') {
            apply_preset(key - '0');
        }
    }

    if (g_vcam_fd >= 0) {
        close(g_vcam_fd);
    }
    cap.release();
    cv::destroyAllWindows();
    std::cout << "[DevControls] Native Camera Stream closed.\n";
    return 0;
}
