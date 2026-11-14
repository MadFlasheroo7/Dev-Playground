#pragma once

#include <vector>
#include <cmath>
#include <algorithm>
#include <cstdint>

class CombFilter {
public:
    void setBufferSize(int32_t size) {
        mBuffer.assign(size, 0.0f);
        mIndex = 0;
        mFilterState = 0.0f;
    }

    inline float process(float in, float feedback, float damp) noexcept {
        const float out = mBuffer[mIndex];
        mFilterState = (out * (1.0f - damp)) + (mFilterState * damp);
        mBuffer[mIndex] = in + (mFilterState * feedback);
        if (++mIndex >= static_cast<int32_t>(mBuffer.size())) {
            mIndex = 0;
        }
        return out;
    }

private:
    std::vector<float> mBuffer;
    int32_t mIndex = 0;
    float mFilterState = 0.0f;
};

class AllpassFilter {
public:
    void setBufferSize(int32_t size) {
        mBuffer.assign(size, 0.0f);
        mIndex = 0;
    }

    inline float process(float in) noexcept {
        const float bufOut = mBuffer[mIndex];
        const float out = -in + bufOut;
        mBuffer[mIndex] = in + (bufOut * 0.5f);
        if (++mIndex >= static_cast<int32_t>(mBuffer.size())) {
            mIndex = 0;
        }
        return out;
    }

private:
    std::vector<float> mBuffer;
    int32_t mIndex = 0;
};

class Freeverb {
public:
    void prepare(float sampleRate) {
        mSampleRate = sampleRate;
        const float scale = sampleRate / 44100.0f;

        const int32_t combTunings[8] = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
        const int32_t allpassTunings[4] = {556, 441, 341, 225};

        for (int i = 0; i < 8; ++i) {
            mCombs[i].setBufferSize(static_cast<int32_t>(combTunings[i] * scale));
        }
        for (int i = 0; i < 4; ++i) {
            mAllpasses[i].setBufferSize(static_cast<int32_t>(allpassTunings[i] * scale));
        }
    }

    void setParameters(float roomSize, float damp, float wetMix) {
        mRoomSize = std::clamp(roomSize, 0.0f, 0.98f);
        mDamp = std::clamp(damp, 0.0f, 1.0f);
        mWetMix = std::clamp(wetMix, 0.0f, 1.0f);
    }

    void processBlock(float* buffer, int32_t numFrames) noexcept {
        if (mWetMix < 0.001f) return;

        const float dryMix = 1.0f - mWetMix;
        const float feedback = mRoomSize;
        const float damp = mDamp;
        const float wet = mWetMix * 0.15f; // Scale down summed 8 comb outputs

        for (int32_t i = 0; i < numFrames; ++i) {
            const float in = buffer[i];
            float combSum = 0.0f;

            for (int c = 0; c < 8; ++c) {
                combSum += mCombs[c].process(in, feedback, damp);
            }

            float diffused = combSum;
            for (int a = 0; a < 4; ++a) {
                diffused = mAllpasses[a].process(diffused);
            }

            buffer[i] = in * dryMix + diffused * wet;
        }
    }

private:
    float mSampleRate = 48000.0f;
    float mRoomSize = 0.5f;
    float mDamp = 0.2f;
    float mWetMix = 0.0f;

    CombFilter mCombs[8];
    AllpassFilter mAllpasses[4];
};
