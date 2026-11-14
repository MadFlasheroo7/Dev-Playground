cmake_minimum_required(VERSION 3.22.1)

project(audio_studio_engine)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

add_library(audio_studio_engine SHARED
    src/main/cpp/engine/AudioEngine.cpp
    src/main/cpp/jni/JniBridge.cpp
)

target_include_directories(audio_studio_engine PRIVATE
    src/main/cpp
)

target_link_libraries(audio_studio_engine PRIVATE
    oboe::oboe
    log
    OpenSLES
    aaudio
)
