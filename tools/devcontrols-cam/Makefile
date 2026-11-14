CXX = g++
CXXFLAGS = -O3 -march=native -Wall -I/usr/include/opencv5 -I/usr/include/onnxruntime
LDFLAGS_CAM = -lopencv_core -lopencv_videoio -lopencv_imgproc -lopencv_highgui -lopencv_photo
LDFLAGS_HUD = -lonnxruntime -lopencv_core -lopencv_videoio -lopencv_imgproc -lopencv_highgui -lopencv_geometry

TARGET_CAM = devcontrols-cam
TARGET_HUD = air-hud

all: $(TARGET_CAM) $(TARGET_HUD)

$(TARGET_CAM): main.cpp
	$(CXX) $(CXXFLAGS) main.cpp $(LDFLAGS_CAM) -o $(TARGET_CAM)

$(TARGET_HUD): air_hud.cpp hand_tracker.cpp palm_anchors.cpp hand_tracker.hpp gesture_engine.hpp palm_anchors.hpp virtual_mouse.hpp
	$(CXX) $(CXXFLAGS) air_hud.cpp hand_tracker.cpp palm_anchors.cpp $(LDFLAGS_HUD) -o $(TARGET_HUD)

clean:
	rm -f $(TARGET_CAM) $(TARGET_HUD)

.PHONY: all clean
