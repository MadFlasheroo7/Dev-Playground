#pragma once
#include "hand_tracker.hpp"
#include "virtual_mouse.hpp"
#include <chrono>
#include <string>
#include <deque>
#include <cstdlib>
#include <cmath>
#include <iostream>

enum class AirGesture {
    NONE = 0,
    OPEN_HAND,
    WAVE_RIGHT,
    WAVE_LEFT,
    FIST,
    TWO_FINGERS,
    PINCH,
    POINT_RIGHT,
    POINT_LEFT,
    POINT_UP,
    POINT_DOWN,
    GRAB_DRAG_RIGHT,
    GRAB_DRAG_LEFT,
    GRAB_DRAG_UP,
    GRAB_DRAG_DOWN,
    ROCK_HORNS,
    THUMBS_UP,
    THUMBS_DOWN
};

struct GestureEvent {
    AirGesture gesture = AirGesture::NONE;
    std::string name = "IDLE";
    std::string action_desc = "";
    bool triggered = false;
    float cooldown_progress = 0.0f; // 0.0 to 1.0 (1.0 = ready)
    std::string last_action_message = "";
    double last_action_timestamp = 0.0;

    // Pinch-to-Scroll fields
    bool is_scrolling = false;
    int scroll_ticks = 0; // positive = up, negative = down
    cv::Point2f pinch_point{0.0f, 0.0f};

    // Point-to-Focus visual ray
    bool is_pointing = false;
    cv::Point2f point_start{0.0f, 0.0f};
    cv::Point2f point_end{0.0f, 0.0f};
    std::string point_dir = "";

    // Grab & Move visual tether
    bool is_grabbing = false;
    bool is_dragging = false;
    cv::Point2f grab_anchor{0.0f, 0.0f};
    cv::Point2f grab_current{0.0f, 0.0f};
    std::string drag_dir = "";
};

class GestureEngine {
private:
    struct PalmHistory {
        float x;
        float y;
        std::chrono::steady_clock::time_point time;
    };

    std::deque<PalmHistory> palm_history;
    AirGesture last_detected = AirGesture::NONE;
    int stable_frames = 0;

    std::chrono::steady_clock::time_point last_trigger_time;
    const std::chrono::milliseconds cooldown_duration{450}; // 450ms debounce
    std::string last_action_msg = "";
    std::chrono::steady_clock::time_point last_action_time;
    AirGesture latched_gesture = AirGesture::NONE;

    // Virtual Mouse for Pinch Scrolling
    VirtualMouse mouse;
    bool in_pinch = false;
    float last_pinch_y = 0.0f;
    float scroll_accumulator = 0.0f;

    // Grab & Move state
    bool in_grab = false;
    cv::Point2f grab_anchor{0.0f, 0.0f};
    std::chrono::steady_clock::time_point grab_start_time;
    bool grab_has_moved = false;
    std::chrono::steady_clock::time_point last_drag_step_time;

    // Volume repeat timer
    std::chrono::steady_clock::time_point last_volume_tick;

    static float dist2d(const cv::Point3f& a, const cv::Point3f& b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return std::sqrt(dx * dx + dy * dy);
    }

    static float dist2d(const cv::Point2f& a, const cv::Point2f& b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return std::sqrt(dx * dx + dy * dy);
    }

    void execute_action(const std::string& cmd) {
        std::string bg_cmd = "(" + cmd + ") >/dev/null 2>&1 &";
        int res = system(bg_cmd.c_str());
        (void)res;
    }

public:
    GestureEngine() {
        last_trigger_time = std::chrono::steady_clock::now() - cooldown_duration;
        last_action_time = std::chrono::steady_clock::now() - std::chrono::seconds(10);
        last_drag_step_time = std::chrono::steady_clock::now() - std::chrono::seconds(10);
        last_volume_tick = std::chrono::steady_clock::now() - std::chrono::seconds(10);
    }

    GestureEvent update(const HandData& hand) {
        GestureEvent event;
        auto now = std::chrono::steady_clock::now();

        auto elapsed_cooldown = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_trigger_time).count();
        bool in_cooldown = (elapsed_cooldown < cooldown_duration.count());
        event.cooldown_progress = std::min(1.0f, static_cast<float>(elapsed_cooldown) / cooldown_duration.count());

        auto action_elapsed_sec = std::chrono::duration<double>(now - last_action_time).count();
        if (action_elapsed_sec < 1.8) {
            event.last_action_message = last_action_msg;
            event.last_action_timestamp = action_elapsed_sec;
        }

        if (!hand.detected || hand.landmarks.size() < 21) {
            palm_history.clear();
            last_detected = AirGesture::NONE;
            latched_gesture = AirGesture::NONE;
            in_pinch = false;
            in_grab = false;
            grab_has_moved = false;
            scroll_accumulator = 0.0f;
            stable_frames = 0;
            event.gesture = AirGesture::NONE;
            event.name = "WAITING FOR HAND";
            return event;
        }

        const auto& lms = hand.landmarks;
        const auto& wrist = lms[0];

        // Scale baseline: Wrist to Middle MCP
        float hand_scale = dist2d(wrist, lms[9]);
        if (hand_scale < 10.0f) hand_scale = 100.0f;

        // Extension check for each finger (Tip further from wrist than PIP joint)
        bool index_extended  = dist2d(lms[8], wrist)  > dist2d(lms[6], wrist) * 1.14f;
        bool middle_extended = dist2d(lms[12], wrist) > dist2d(lms[10], wrist) * 1.14f;
        bool ring_extended   = dist2d(lms[16], wrist) > dist2d(lms[14], wrist) * 1.14f;
        bool pinky_extended  = dist2d(lms[20], wrist) > dist2d(lms[18], wrist) * 1.14f;

        // Thumb extension relative to Pinky MCP (17)
        bool thumb_extended = dist2d(lms[4], lms[17]) > dist2d(lms[3], lms[17]) * 1.18f;

        int extended_count = (index_extended ? 1 : 0) +
                             (middle_extended ? 1 : 0) +
                             (ring_extended ? 1 : 0) +
                             (pinky_extended ? 1 : 0);

        // Pinch distance between thumb tip (4) and index tip (8)
        float pinch_dist = dist2d(lms[4], lms[8]) / hand_scale;
        bool is_pinch = in_pinch ? (pinch_dist < 0.38f) : (pinch_dist < 0.26f);

        // Update Palm Motion History for Swipes
        palm_history.push_back({hand.palm_center.x, hand.palm_center.y, now});
        while (!palm_history.empty()) {
            auto dt = std::chrono::duration_cast<std::chrono::milliseconds>(now - palm_history.front().time).count();
            if (dt > 350) {
                palm_history.pop_front();
            } else {
                break;
            }
        }

        float swipe_dx = 0.0f;
        if (palm_history.size() >= 3) {
            swipe_dx = palm_history.back().x - palm_history.front().x;
        }

        // =========================================================================
        // 1. PINCH TO SCROLL (Continuous High-Res Virtual Wheel)
        // =========================================================================
        if (is_pinch) {
            in_grab = false;
            grab_has_moved = false;
            cv::Point3f p_thumb = lms[4];
            cv::Point3f p_index = lms[8];
            cv::Point2f pinch_pt((p_thumb.x + p_index.x) * 0.5f, (p_thumb.y + p_index.y) * 0.5f);

            if (!in_pinch) {
                in_pinch = true;
                last_pinch_y = pinch_pt.y;
                scroll_accumulator = 0.0f;
            } else {
                float dy = last_pinch_y - pinch_pt.y;
                last_pinch_y = pinch_pt.y;
                scroll_accumulator += dy;

                float scroll_step = std::max(5.0f, hand_scale * 0.05f);

                if (scroll_accumulator >= scroll_step) {
                    int steps = static_cast<int>(scroll_accumulator / scroll_step);
                    mouse.scroll(steps);
                    scroll_accumulator -= steps * scroll_step;
                    event.scroll_ticks = steps;
                    last_action_msg = "▲ SCROLL UP";
                    last_action_time = now;
                } else if (scroll_accumulator <= -scroll_step) {
                    int steps = static_cast<int>(scroll_accumulator / scroll_step);
                    mouse.scroll(steps);
                    scroll_accumulator -= steps * scroll_step;
                    event.scroll_ticks = steps;
                    last_action_msg = "▼ SCROLL DOWN";
                    last_action_time = now;
                }
            }

            event.gesture = AirGesture::PINCH;
            event.is_scrolling = true;
            event.pinch_point = pinch_pt;
            event.name = "PINCH SCROLL";
            event.action_desc = "Drag Up/Down to Scroll";
            return event;
        } else {
            in_pinch = false;
            scroll_accumulator = 0.0f;
        }

        // =========================================================================
        // 2. GESTURE RECOGNITION & CLASSIFICATION
        // =========================================================================
        AirGesture current_gesture = AirGesture::NONE;

        // A. POINT TO FOCUS (Index extended, others curled)
        bool is_pointing_pose = index_extended && (!middle_extended) && (!ring_extended) && (!pinky_extended);
        if (is_pointing_pose) {
            in_grab = false;
            grab_has_moved = false;

            cv::Point2f idx_mcp(lms[5].x, lms[5].y);
            cv::Point2f idx_tip(lms[8].x, lms[8].y);
            float pdx = idx_tip.x - idx_mcp.x;
            float pdy = idx_tip.y - idx_mcp.y;

            event.is_pointing = true;
            event.point_start = idx_mcp;
            event.point_end = idx_tip + cv::Point2f(pdx * 0.45f, pdy * 0.45f);

            // Dominant direction
            if (std::abs(pdx) >= std::abs(pdy) * 1.12f) {
                if (pdx > 0) {
                    current_gesture = AirGesture::POINT_RIGHT;
                    event.point_dir = "RIGHT";
                } else {
                    current_gesture = AirGesture::POINT_LEFT;
                    event.point_dir = "LEFT";
                }
            } else if (std::abs(pdy) >= std::abs(pdx) * 1.12f) {
                if (pdy < 0) {
                    current_gesture = AirGesture::POINT_UP;
                    event.point_dir = "UP";
                } else {
                    current_gesture = AirGesture::POINT_DOWN;
                    event.point_dir = "DOWN";
                }
            }
        }
        // B. GRAB / PICK & MOVE WINDOW (Closed Fist + Kinetic Displacement)
        else if (extended_count == 0 && !thumb_extended) {
            cv::Point2f cur_palm(hand.palm_center.x, hand.palm_center.y);

            if (!in_grab) {
                in_grab = true;
                grab_anchor = cur_palm;
                grab_start_time = now;
                grab_has_moved = false;
                last_drag_step_time = now;
            }

            event.is_grabbing = true;
            event.grab_anchor = grab_anchor;
            event.grab_current = cur_palm;

            float gdx = cur_palm.x - grab_anchor.x;
            float gdy = cur_palm.y - grab_anchor.y;
            float drag_dist = std::sqrt(gdx * gdx + gdy * gdy);
            float drag_threshold = hand_scale * 0.30f; // Kinetic threshold

            auto drag_step_elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_drag_step_time).count();

            if (drag_dist > drag_threshold && drag_step_elapsed > 350) {
                grab_has_moved = true;
                event.is_dragging = true;

                if (std::abs(gdx) >= std::abs(gdy)) {
                    if (gdx > 0) {
                        current_gesture = AirGesture::GRAB_DRAG_RIGHT;
                        event.drag_dir = "RIGHT";
                    } else {
                        current_gesture = AirGesture::GRAB_DRAG_LEFT;
                        event.drag_dir = "LEFT";
                    }
                } else {
                    if (gdy < 0) {
                        current_gesture = AirGesture::GRAB_DRAG_UP;
                        event.drag_dir = "UP";
                    } else {
                        current_gesture = AirGesture::GRAB_DRAG_DOWN;
                        event.drag_dir = "DOWN";
                    }
                }

                grab_anchor = cur_palm;
                last_drag_step_time = now;
            } else if (!grab_has_moved) {
                auto hold_ms = std::chrono::duration_cast<std::chrono::milliseconds>(now - grab_start_time).count();
                if (hold_ms > 750) {
                    current_gesture = AirGesture::FIST;
                } else {
                    current_gesture = AirGesture::NONE;
                    event.name = "GRAB READY";
                    event.action_desc = "Drag Fist to Move Window";
                }
            }
        }
        else {
            in_grab = false;
            grab_has_moved = false;

            // C. THUMBS UP / DOWN (Volume Control)
            if (thumb_extended && extended_count == 0) {
                float tdy = lms[4].y - lms[2].y;
                if (std::abs(tdy) > hand_scale * 0.20f) {
                    if (tdy < 0) {
                        current_gesture = AirGesture::THUMBS_UP;
                    } else {
                        current_gesture = AirGesture::THUMBS_DOWN;
                    }
                }
            }
            // D. ROCK / HORNS (Index + Pinky extended -> Fullscreen Toggle)
            else if (index_extended && pinky_extended && (!middle_extended) && (!ring_extended)) {
                current_gesture = AirGesture::ROCK_HORNS;
            }
            // E. TWO FINGERS (V-Sign -> Screenshot)
            else if (index_extended && middle_extended && (!ring_extended) && (!pinky_extended)) {
                current_gesture = AirGesture::TWO_FINGERS;
            }
            // F. OPEN HAND SWIPE (Horizontal Velocity -> Workspace switch)
            else if (extended_count >= 3) {
                float swipe_threshold = hand_scale * 0.40f;
                if (swipe_dx > swipe_threshold) {
                    current_gesture = AirGesture::WAVE_RIGHT;
                } else if (swipe_dx < -swipe_threshold) {
                    current_gesture = AirGesture::WAVE_LEFT;
                } else {
                    current_gesture = AirGesture::OPEN_HAND;
                }
            }
        }

        // Stability filtering
        if (current_gesture == last_detected && current_gesture != AirGesture::NONE) {
            stable_frames++;
        } else {
            last_detected = current_gesture;
            stable_frames = 1;
        }

        event.gesture = current_gesture;

        // Label and description mapping
        switch (current_gesture) {
            case AirGesture::POINT_RIGHT:
                event.name = "POINT RIGHT";
                event.action_desc = "Focus Right Window";
                break;
            case AirGesture::POINT_LEFT:
                event.name = "POINT LEFT";
                event.action_desc = "Focus Left Window";
                break;
            case AirGesture::POINT_UP:
                event.name = "POINT UP";
                event.action_desc = "Focus Upper Window";
                break;
            case AirGesture::POINT_DOWN:
                event.name = "POINT DOWN";
                event.action_desc = "Focus Lower Window";
                break;
            case AirGesture::GRAB_DRAG_RIGHT:
                event.name = "DRAG RIGHT";
                event.action_desc = "Swap Window Right";
                break;
            case AirGesture::GRAB_DRAG_LEFT:
                event.name = "DRAG LEFT";
                event.action_desc = "Swap Window Left";
                break;
            case AirGesture::GRAB_DRAG_UP:
                event.name = "DRAG UP";
                event.action_desc = "Swap Window Up";
                break;
            case AirGesture::GRAB_DRAG_DOWN:
                event.name = "DRAG DOWN";
                event.action_desc = "Swap Window Down";
                break;
            case AirGesture::FIST:
                event.name = "FIST HOLD";
                event.action_desc = "Mute Mic & Pause Media";
                break;
            case AirGesture::ROCK_HORNS:
                event.name = "ROCK HORNS";
                event.action_desc = "Toggle Fullscreen";
                break;
            case AirGesture::THUMBS_UP:
                event.name = "THUMBS UP";
                event.action_desc = "Volume +5%";
                break;
            case AirGesture::THUMBS_DOWN:
                event.name = "THUMBS DOWN";
                event.action_desc = "Volume -5%";
                break;
            case AirGesture::TWO_FINGERS:
                event.name = "TWO FINGERS";
                event.action_desc = "Capture Screenshot";
                break;
            case AirGesture::WAVE_RIGHT:
                event.name = "SWIPE RIGHT";
                event.action_desc = "Next Workspace";
                break;
            case AirGesture::WAVE_LEFT:
                event.name = "SWIPE LEFT";
                event.action_desc = "Prev Workspace";
                break;
            case AirGesture::OPEN_HAND:
                event.name = "HAND OPEN";
                event.action_desc = "Ready to Swipe";
                break;
            default:
                if (event.name == "IDLE") {
                    event.name = "TRACKING";
                }
                break;
        }

        // Reset latches on pose changes
        if (current_gesture != latched_gesture) {
            latched_gesture = AirGesture::NONE;
        }

        // =========================================================================
        // 3. ACTION DISPATCHING (Hyprland / Linux)
        // =========================================================================
        bool is_grab_drag = (current_gesture == AirGesture::GRAB_DRAG_RIGHT ||
                             current_gesture == AirGesture::GRAB_DRAG_LEFT  ||
                             current_gesture == AirGesture::GRAB_DRAG_UP    ||
                             current_gesture == AirGesture::GRAB_DRAG_DOWN);

        bool is_volume = (current_gesture == AirGesture::THUMBS_UP ||
                          current_gesture == AirGesture::THUMBS_DOWN);

        // Special handling for volume continuous stepped repeat (every 320ms)
        if (is_volume && stable_frames >= 2) {
            auto vol_dt = std::chrono::duration_cast<std::chrono::milliseconds>(now - last_volume_tick).count();
            if (vol_dt >= 320) {
                if (current_gesture == AirGesture::THUMBS_UP) {
                    execute_action("wpctl set-volume @DEFAULT_AUDIO_SINK@ 5%+");
                    last_action_msg = "🔊 VOLUME +5%";
                } else {
                    execute_action("wpctl set-volume @DEFAULT_AUDIO_SINK@ 5%-");
                    last_action_msg = "🔉 VOLUME -5%";
                }
                last_volume_tick = now;
                last_action_time = now;
                event.triggered = true;
                event.last_action_message = last_action_msg;
                event.last_action_timestamp = 0.0;
            }
        }
        // Discrete actions (Debounced + Latched)
        else if (stable_frames >= 2 && current_gesture != AirGesture::NONE && current_gesture != AirGesture::OPEN_HAND) {
            bool can_trigger = is_grab_drag ? true : (!in_cooldown && current_gesture != latched_gesture);

            if (can_trigger) {
                bool did_trigger = false;

                switch (current_gesture) {
                    case AirGesture::POINT_RIGHT:
                        execute_action("hyprctl dispatch movefocus r");
                        last_action_msg = "👉 FOCUS RIGHT";
                        did_trigger = true;
                        break;
                    case AirGesture::POINT_LEFT:
                        execute_action("hyprctl dispatch movefocus l");
                        last_action_msg = "👈 FOCUS LEFT";
                        did_trigger = true;
                        break;
                    case AirGesture::POINT_UP:
                        execute_action("hyprctl dispatch movefocus u");
                        last_action_msg = "👆 FOCUS UP";
                        did_trigger = true;
                        break;
                    case AirGesture::POINT_DOWN:
                        execute_action("hyprctl dispatch movefocus d");
                        last_action_msg = "👇 FOCUS DOWN";
                        did_trigger = true;
                        break;
                    case AirGesture::GRAB_DRAG_RIGHT:
                        execute_action("hyprctl dispatch swapwindow r");
                        last_action_msg = "✊ MOVE WINDOW RIGHT";
                        did_trigger = true;
                        break;
                    case AirGesture::GRAB_DRAG_LEFT:
                        execute_action("hyprctl dispatch swapwindow l");
                        last_action_msg = "✊ MOVE WINDOW LEFT";
                        did_trigger = true;
                        break;
                    case AirGesture::GRAB_DRAG_UP:
                        execute_action("hyprctl dispatch swapwindow u");
                        last_action_msg = "✊ MOVE WINDOW UP";
                        did_trigger = true;
                        break;
                    case AirGesture::GRAB_DRAG_DOWN:
                        execute_action("hyprctl dispatch swapwindow d");
                        last_action_msg = "✊ MOVE WINDOW DOWN";
                        did_trigger = true;
                        break;
                    case AirGesture::FIST:
                        execute_action("wpctl set-mute @DEFAULT_AUDIO_SOURCE@ toggle; playerctl play-pause");
                        last_action_msg = "🔇 MIC TOGGLED / PAUSED";
                        did_trigger = true;
                        break;
                    case AirGesture::ROCK_HORNS:
                        execute_action("hyprctl dispatch fullscreen 1");
                        last_action_msg = "🪟 FULLSCREEN TOGGLED";
                        did_trigger = true;
                        break;
                    case AirGesture::TWO_FINGERS:
                        execute_action("mkdir -p ~/Pictures/Screenshots && grim ~/Pictures/Screenshots/screenshot_$(date +%Y%m%d_%H%M%S).png && notify-send -i camera-photo \"DevControls Air HUD\" \"Desktop Screenshot Captured!\"");
                        last_action_msg = "📸 SCREENSHOT SAVED";
                        did_trigger = true;
                        break;
                    case AirGesture::WAVE_RIGHT:
                        execute_action("hyprctl dispatch workspace e+1");
                        last_action_msg = "➡️ NEXT WORKSPACE";
                        did_trigger = true;
                        break;
                    case AirGesture::WAVE_LEFT:
                        execute_action("hyprctl dispatch workspace e-1");
                        last_action_msg = "⬅️ PREV WORKSPACE";
                        did_trigger = true;
                        break;
                    default:
                        break;
                }

                if (did_trigger) {
                    event.triggered = true;
                    last_trigger_time = now;
                    last_action_time = now;
                    event.last_action_message = last_action_msg;
                    event.last_action_timestamp = 0.0;
                    event.cooldown_progress = 0.0f;
                    latched_gesture = current_gesture;
                    palm_history.clear();
                }
            }
        }

        return event;
    }
};
