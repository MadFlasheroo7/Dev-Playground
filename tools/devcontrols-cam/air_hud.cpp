#include <opencv2/opencv.hpp>
#include <iostream>
#include <chrono>
#include <thread>
#include <vector>
#include <cmath>
#include "hand_tracker.hpp"
#include "gesture_engine.hpp"

// Hyprland window title
const std::string HUD_WINDOW_NAME = "DevControls Air HUD";

void setup_hyprland_rules() {
    // Inject dynamic window rules so Hyprland treats this window as a floating desktop overlay
    system("hyprctl keyword windowrule \"float 1, match:title DevControls Air HUD\" > /dev/null 2>&1");
    system("hyprctl keyword windowrule \"pin 1, match:title DevControls Air HUD\" > /dev/null 2>&1");
    system("hyprctl keyword windowrule \"move 75% 5%, match:title DevControls Air HUD\" > /dev/null 2>&1");
    system("hyprctl keyword windowrule \"size 400 225, match:title DevControls Air HUD\" > /dev/null 2>&1");
    system("hyprctl keyword windowrule \"opacity 0.95 0.90, match:title DevControls Air HUD\" > /dev/null 2>&1");
}

void draw_cyber_bone(cv::Mat& canvas, const cv::Point& p1, const cv::Point& p2, const cv::Scalar& color, int thickness = 2) {
    if (p1.x <= 0 || p1.y <= 0 || p2.x <= 0 || p2.y <= 0) return;
    cv::line(canvas, p1, p2, color, thickness, cv::LINE_AA);
}

void draw_cyber_joint(cv::Mat& canvas, const cv::Point& pt, const cv::Scalar& color, int radius = 3) {
    if (pt.x <= 0 || pt.y <= 0) return;
    cv::circle(canvas, pt, radius + 2, cv::Scalar(0, 0, 0), -1, cv::LINE_AA);
    cv::circle(canvas, pt, radius, color, -1, cv::LINE_AA);
    cv::circle(canvas, pt, radius / 2, cv::Scalar(255, 255, 255), -1, cv::LINE_AA);
}

cv::VideoCapture open_webcam(int& detected_index) {
    for (int i = 0; i < 10; ++i) {
        cv::VideoCapture test_cap(i, cv::CAP_V4L2);
        if (test_cap.isOpened()) {
            test_cap.set(cv::CAP_PROP_FOURCC, cv::VideoWriter::fourcc('M', 'J', 'P', 'G'));
            test_cap.set(cv::CAP_PROP_FRAME_WIDTH, 1280);
            test_cap.set(cv::CAP_PROP_FRAME_HEIGHT, 720);
            test_cap.set(cv::CAP_PROP_FPS, 60);
            test_cap.set(cv::CAP_PROP_BUFFERSIZE, 1);

            cv::Mat test_frame;
            for (int retry = 0; retry < 5; ++retry) {
                test_cap >> test_frame;
                if (!test_frame.empty()) {
                    detected_index = i;
                    std::cout << "[Hardware] Connected to camera at /dev/video" << i << " (1280x720 @ 60 FPS MJPEG)\n";
                    return test_cap;
                }
            }
            test_cap.release();
        }
    }
    detected_index = -1;
    return cv::VideoCapture();
}

int main(int argc, char** argv) {
    std::cout << "========================================================\n";
    std::cout << "  DevControls Air-HUD Gesture Controller (Linux / C++)\n";
    std::cout << "========================================================\n";

    setup_hyprland_rules();

    std::cout << "[Init] Loading MediaPipe ONNX models via Microsoft ONNX Runtime...\n";
    HandTracker tracker("models/palm_detection.onnx", "models/handpose_estimation.onnx");
    GestureEngine engine;
    std::cout << "[Init] Gesture Engine initialized.\n";

    // Auto-detect and open physical webcam (waits if unplugged)
    int cam_idx = -1;
    cv::VideoCapture cap = open_webcam(cam_idx);
    while (!cap.isOpened()) {
        std::cout << "[Hardware] Waiting for webcam to be connected on /dev/video*...\n";
        std::this_thread::sleep_for(std::chrono::seconds(2));
        cap = open_webcam(cam_idx);
    }

    const int hud_w = 400;
    const int hud_h = 225; // Exact 16:9 native aspect ratio

    cv::namedWindow(HUD_WINDOW_NAME, cv::WINDOW_NORMAL);
    cv::resizeWindow(HUD_WINDOW_NAME, hud_w, hud_h);

    cv::Mat raw_frame, mirrored_frame, enhanced_frame, hud_canvas;
    cv::Mat lab;
    std::vector<cv::Mat> planes(3);
    cv::Ptr<cv::CLAHE> clahe = cv::createCLAHE(3.5, cv::Size(8, 8));

    auto last_fps_time = std::chrono::high_resolution_clock::now();
    int frame_count = 0;
    double fps = 60.0;
    int empty_frames = 0;

    bool show_skeleton = true;
    bool enable_enhancement = true;

    std::cout << "\n[Controls]\n";
    std::cout << "  [H] Toggle Skeletal Nodes\n";
    std::cout << "  [C] Toggle Low-Light Auto-Enhancement\n";
    std::cout << "  [ESC/Q] Exit Air-HUD\n\n";

    while (true) {
        cap >> raw_frame;
        if (raw_frame.empty()) {
            empty_frames++;
            if (empty_frames > 20) {
                std::cout << "[Hardware] Re-scanning for camera...\n";
                cap.release();
                cap = open_webcam(cam_idx);
                empty_frames = 0;
            }
            continue;
        }
        empty_frames = 0;

        frame_count++;
        auto now = std::chrono::high_resolution_clock::now();
        double elapsed_sec = std::chrono::duration<double>(now - last_fps_time).count();
        if (elapsed_sec >= 0.5) {
            fps = frame_count / elapsed_sec;
            frame_count = 0;
            last_fps_time = now;
        }

        // Mirror camera frame horizontally for natural mirror interaction
        cv::flip(raw_frame, mirrored_frame, 1);

        // Low-Light Brightness & Contrast Lift for Night/Dim Rooms
        if (enable_enhancement) {
            cv::cvtColor(mirrored_frame, lab, cv::COLOR_BGR2Lab);
            cv::split(lab, planes);
            clahe->apply(planes[0], planes[0]);
            cv::merge(planes, lab);
            cv::cvtColor(lab, enhanced_frame, cv::COLOR_Lab2BGR);
            enhanced_frame.convertTo(enhanced_frame, -1, 1.20, 18);
        } else {
            enhanced_frame = mirrored_frame.clone();
        }

        // Process hand pose inference on enhanced frame for maximum tracking accuracy
        HandData hand = tracker.process(enhanced_frame);
        GestureEvent event = engine.update(hand);

        // Render full vibrant camera feed directly onto HUD canvas
        cv::resize(enhanced_frame, hud_canvas, cv::Size(hud_w, hud_h));

        // Scale factors to map 1280x720 landmarks to 400x225 HUD
        float sx = static_cast<float>(hud_w) / mirrored_frame.cols;
        float sy = static_cast<float>(hud_h) / mirrored_frame.rows;

        // Draw Skeletal Joints & Bones directly on top of the live video
        if (hand.detected && show_skeleton && hand.landmarks.size() == 21) {
            std::vector<cv::Point> pts(21);
            for (int i = 0; i < 21; ++i) {
                pts[i] = cv::Point(static_cast<int>(hand.landmarks[i].x * sx),
                                   static_cast<int>(hand.landmarks[i].y * sy));
            }

            // Colors for fingers
            cv::Scalar col_palm(220, 220, 220);
            cv::Scalar col_thumb(0, 210, 255);    // Gold / Amber
            cv::Scalar col_index(0, 255, 120);    // Neon Green
            cv::Scalar col_middle(255, 230, 0);   // Electric Cyan
            cv::Scalar col_ring(255, 120, 0);     // Vibrant Blue
            cv::Scalar col_pinky(220, 0, 255);    // Magenta

            // Palm base connections
            draw_cyber_bone(hud_canvas, pts[0], pts[1], col_palm, 2);
            draw_cyber_bone(hud_canvas, pts[0], pts[5], col_palm, 2);
            draw_cyber_bone(hud_canvas, pts[5], pts[9], col_palm, 2);
            draw_cyber_bone(hud_canvas, pts[9], pts[13], col_palm, 2);
            draw_cyber_bone(hud_canvas, pts[13], pts[17], col_palm, 2);
            draw_cyber_bone(hud_canvas, pts[0], pts[17], col_palm, 2);

            // Thumb
            draw_cyber_bone(hud_canvas, pts[1], pts[2], col_thumb, 2);
            draw_cyber_bone(hud_canvas, pts[2], pts[3], col_thumb, 2);
            draw_cyber_bone(hud_canvas, pts[3], pts[4], col_thumb, 2);

            // Index
            draw_cyber_bone(hud_canvas, pts[5], pts[6], col_index, 2);
            draw_cyber_bone(hud_canvas, pts[6], pts[7], col_index, 2);
            draw_cyber_bone(hud_canvas, pts[7], pts[8], col_index, 2);

            // Middle
            draw_cyber_bone(hud_canvas, pts[9], pts[10], col_middle, 2);
            draw_cyber_bone(hud_canvas, pts[10], pts[11], col_middle, 2);
            draw_cyber_bone(hud_canvas, pts[11], pts[12], col_middle, 2);

            // Ring
            draw_cyber_bone(hud_canvas, pts[13], pts[14], col_ring, 2);
            draw_cyber_bone(hud_canvas, pts[14], pts[15], col_ring, 2);
            draw_cyber_bone(hud_canvas, pts[15], pts[16], col_ring, 2);

            // Pinky
            draw_cyber_bone(hud_canvas, pts[17], pts[18], col_pinky, 2);
            draw_cyber_bone(hud_canvas, pts[18], pts[19], col_pinky, 2);
            draw_cyber_bone(hud_canvas, pts[19], pts[20], col_pinky, 2);

            // Draw joint nodes
            for (int i = 0; i < 21; ++i) {
                cv::Scalar jcol = col_palm;
                if (i >= 1 && i <= 4) jcol = col_thumb;
                else if (i >= 5 && i <= 8) jcol = col_index;
                else if (i >= 9 && i <= 12) jcol = col_middle;
                else if (i >= 13 && i <= 16) jcol = col_ring;
                else if (i >= 17 && i <= 20) jcol = col_pinky;

                int rad = (i == 4 || i == 8 || i == 12 || i == 16 || i == 20) ? 4 : 2;
                draw_cyber_joint(hud_canvas, pts[i], jcol, rad);
            }

            // Draw Palm Center Target
            cv::Point pc(static_cast<int>(hand.palm_center.x * sx), static_cast<int>(hand.palm_center.y * sy));
            cv::circle(hud_canvas, pc, 8, cv::Scalar(0, 255, 255), 1, cv::LINE_AA);
            cv::drawMarker(hud_canvas, pc, cv::Scalar(0, 255, 255), cv::MARKER_CROSS, 12, 1, cv::LINE_AA);
        }

        // =========================================================================
        // Dynamic Gesture Visual Cues
        // =========================================================================

        // 1. Interactive Scroll Reticle when Pinch Scrolling is Active
        if (event.is_scrolling && event.pinch_point.x > 0) {
            cv::Point pp(static_cast<int>(event.pinch_point.x * sx),
                         static_cast<int>(event.pinch_point.y * sy));
            cv::circle(hud_canvas, pp, 14, cv::Scalar(0, 180, 255), 2, cv::LINE_AA);
            cv::circle(hud_canvas, pp, 5, cv::Scalar(0, 255, 255), -1, cv::LINE_AA);
            cv::line(hud_canvas, cv::Point(pp.x, pp.y - 14), cv::Point(pp.x, pp.y - 30), cv::Scalar(0, 255, 255), 2, cv::LINE_AA);
            cv::putText(hud_canvas, "^", cv::Point(pp.x - 5, pp.y - 30), cv::FONT_HERSHEY_DUPLEX, 0.5, cv::Scalar(0, 255, 255), 1, cv::LINE_AA);
            cv::line(hud_canvas, cv::Point(pp.x, pp.y + 14), cv::Point(pp.x, pp.y + 30), cv::Scalar(0, 255, 255), 2, cv::LINE_AA);
            cv::putText(hud_canvas, "v", cv::Point(pp.x - 5, pp.y + 36), cv::FONT_HERSHEY_DUPLEX, 0.42, cv::Scalar(0, 255, 255), 1, cv::LINE_AA);
        }

        // 2. Pointing Laser Vector for Window Focus
        if (event.is_pointing && event.point_end.x > 0) {
            cv::Point p1(static_cast<int>(event.point_start.x * sx),
                         static_cast<int>(event.point_start.y * sy));
            cv::Point p2(static_cast<int>(event.point_end.x * sx),
                         static_cast<int>(event.point_end.y * sy));

            cv::line(hud_canvas, p1, p2, cv::Scalar(0, 255, 120), 2, cv::LINE_AA);
            cv::circle(hud_canvas, p2, 6, cv::Scalar(0, 255, 255), 1, cv::LINE_AA);
            cv::circle(hud_canvas, p2, 2, cv::Scalar(255, 255, 255), -1, cv::LINE_AA);

            if (!event.point_dir.empty()) {
                cv::putText(hud_canvas, event.point_dir, cv::Point(p2.x + 8, p2.y + 4),
                            cv::FONT_HERSHEY_DUPLEX, 0.40, cv::Scalar(0, 255, 120), 1, cv::LINE_AA);
            }
        }

        // 3. Tether Anchor & Displacement Vector for Pick & Move Window
        if (event.is_grabbing && event.grab_anchor.x > 0) {
            cv::Point p_anc(static_cast<int>(event.grab_anchor.x * sx),
                            static_cast<int>(event.grab_anchor.y * sy));
            cv::Point p_cur(static_cast<int>(event.grab_current.x * sx),
                            static_cast<int>(event.grab_current.y * sy));

            cv::circle(hud_canvas, p_anc, 10, cv::Scalar(0, 140, 255), 1, cv::LINE_AA);
            cv::circle(hud_canvas, p_anc, 3, cv::Scalar(0, 140, 255), -1, cv::LINE_AA);
            cv::line(hud_canvas, p_anc, p_cur, cv::Scalar(0, 180, 255), 2, cv::LINE_AA);
            cv::circle(hud_canvas, p_cur, 6, cv::Scalar(0, 255, 255), -1, cv::LINE_AA);

            if (!event.drag_dir.empty()) {
                cv::putText(hud_canvas, "SWAP " + event.drag_dir, cv::Point(p_cur.x + 10, p_cur.y + 4),
                            cv::FONT_HERSHEY_DUPLEX, 0.40, cv::Scalar(0, 220, 255), 1, cv::LINE_AA);
            }
        }

        // =========================================================================
        // Semi-Transparent Glass Overlays
        // =========================================================================

        // 1. Top Header Glass Bar
        cv::Rect top_bar_rect(0, 0, hud_w, 26);
        cv::Mat glass_bar = hud_canvas(top_bar_rect);
        cv::Mat glass_tint(top_bar_rect.size(), CV_8UC3, cv::Scalar(15, 18, 22));
        cv::addWeighted(glass_tint, 0.75, glass_bar, 0.25, 0, glass_bar);
        cv::line(hud_canvas, cv::Point(0, 26), cv::Point(hud_w, 26), cv::Scalar(0, 240, 255), 1);

        cv::putText(hud_canvas, "AIR-HUD", cv::Point(10, 18), cv::FONT_HERSHEY_DUPLEX, 0.48, cv::Scalar(0, 240, 255), 1, cv::LINE_AA);

        char fps_buf[32];
        snprintf(fps_buf, sizeof(fps_buf), "%.0f FPS", fps);
        cv::circle(hud_canvas, cv::Point(hud_w - 70, 13), 3, cv::Scalar(0, 255, 120), -1, cv::LINE_AA);
        cv::putText(hud_canvas, fps_buf, cv::Point(hud_w - 60, 18), cv::FONT_HERSHEY_DUPLEX, 0.42, cv::Scalar(220, 240, 240), 1, cv::LINE_AA);

        // 2. Gesture Pill / Status Badge
        cv::Scalar badge_color(30, 35, 40);
        cv::Scalar text_color(240, 240, 240);

        if (event.is_scrolling) {
            badge_color = cv::Scalar(0, 180, 255); // Amber Gold
            text_color = cv::Scalar(0, 0, 0);
        } else if (event.is_pointing) {
            badge_color = cv::Scalar(0, 220, 100); // Neon Emerald
            text_color = cv::Scalar(0, 0, 0);
        } else if (event.is_grabbing || event.is_dragging) {
            badge_color = cv::Scalar(0, 140, 255); // Vivid Orange
            text_color = cv::Scalar(0, 0, 0);
        } else if (event.gesture == AirGesture::ROCK_HORNS) {
            badge_color = cv::Scalar(240, 0, 180); // Electric Magenta
            text_color = cv::Scalar(255, 255, 255);
        } else if (event.gesture == AirGesture::THUMBS_UP || event.gesture == AirGesture::THUMBS_DOWN) {
            badge_color = cv::Scalar(0, 220, 180); // Teal
            text_color = cv::Scalar(0, 0, 0);
        } else if (event.gesture == AirGesture::WAVE_RIGHT || event.gesture == AirGesture::WAVE_LEFT) {
            badge_color = cv::Scalar(255, 200, 0); // Cyan
            text_color = cv::Scalar(0, 0, 0);
        } else if (event.gesture == AirGesture::FIST) {
            badge_color = cv::Scalar(30, 30, 230); // Crimson
            text_color = cv::Scalar(255, 255, 255);
        } else if (event.gesture == AirGesture::TWO_FINGERS) {
            badge_color = cv::Scalar(240, 40, 190); // Purple
            text_color = cv::Scalar(255, 255, 255);
        } else if (event.gesture == AirGesture::OPEN_HAND) {
            badge_color = cv::Scalar(0, 180, 100);
            text_color = cv::Scalar(255, 255, 255);
        }

        cv::Rect badge_rect(10, 32, hud_w - 20, 24);
        cv::Mat badge_region = hud_canvas(badge_rect);
        cv::Mat badge_tint(badge_rect.size(), CV_8UC3, badge_color);
        cv::addWeighted(badge_tint, 0.82, badge_region, 0.18, 0, badge_region);
        cv::rectangle(hud_canvas, badge_rect, cv::Scalar(255, 255, 255), 1, cv::LINE_AA);

        std::string badge_label = event.name;
        if (!event.action_desc.empty()) {
            badge_label += " : " + event.action_desc;
        }
        cv::putText(hud_canvas, badge_label, cv::Point(badge_rect.x + 8, badge_rect.y + 17),
                    cv::FONT_HERSHEY_DUPLEX, 0.38, text_color, 1, cv::LINE_AA);

        // 3. Action Trigger Notification Flash Banner
        if (!event.last_action_message.empty()) {
            double alpha = 1.0 - (event.last_action_timestamp / 1.8);
            if (alpha > 0.0) {
                int banner_y = hud_h / 2 - 18;
                cv::Rect banner_rect(15, banner_y, hud_w - 30, 36);

                cv::Mat overlay = hud_canvas.clone();
                cv::rectangle(overlay, banner_rect, cv::Scalar(0, 255, 200), -1);
                cv::addWeighted(overlay, alpha * 0.85, hud_canvas, 1.0 - (alpha * 0.85), 0, hud_canvas);

                cv::rectangle(hud_canvas, banner_rect, cv::Scalar(255, 255, 255), 1, cv::LINE_AA);
                cv::putText(hud_canvas, event.last_action_message, cv::Point(banner_rect.x + 12, banner_rect.y + 24),
                            cv::FONT_HERSHEY_DUPLEX, 0.52, cv::Scalar(0, 0, 0), 2, cv::LINE_AA);
            }
        }

        // 4. Cooldown Progress Bar (Bottom)
        cv::rectangle(hud_canvas, cv::Rect(0, hud_h - 8, hud_w, 8), cv::Scalar(20, 25, 30), -1);
        int bar_w = static_cast<int>(hud_w * event.cooldown_progress);
        cv::Scalar bar_color = (event.cooldown_progress >= 1.0f) ? cv::Scalar(0, 255, 120) : cv::Scalar(0, 180, 255);
        if (bar_w > 0) {
            cv::rectangle(hud_canvas, cv::Rect(0, hud_h - 8, bar_w, 8), bar_color, -1);
        }

        // 5. Border glow around HUD window
        cv::rectangle(hud_canvas, cv::Rect(0, 0, hud_w - 1, hud_h - 1), cv::Scalar(0, 240, 255), 1);

        cv::imshow(HUD_WINDOW_NAME, hud_canvas);

        int key = cv::waitKey(1) & 0xFF;
        if (key == 27 || key == 'q' || key == 'Q') {
            break;
        } else if (key == 'h' || key == 'H') {
            show_skeleton = !show_skeleton;
        } else if (key == 'c' || key == 'C') {
            enable_enhancement = !enable_enhancement;
            std::cout << "[Air-HUD] Low-light enhancement: " << (enable_enhancement ? "ON" : "OFF") << "\n";
        }
    }

    cap.release();
    cv::destroyAllWindows();
    std::cout << "[DevControls] Air-HUD Controller stopped.\n";
    return 0;
}
