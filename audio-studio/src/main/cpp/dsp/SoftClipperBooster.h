#pragma once

#include <cmath>
#include <algorithm>
#include <cstdint>

class SoftClipperBooster {
public:
    void setParameters(float boostDb, float driveAmount, float sampleRate) {
        mTargetGain = std::pow(10.0f, boostDb * 0.05f);
        mDrive = std::max(0.1f, driveAmount);
        mNormFactor = 1.0f / fastTanh(mDrive);
        // 5 ms 1-pole smoothing coefficient to prevent zipper noise
        mSmoothAlpha = 1.0f - std::exp(-1.0f / (0.005f * sampleRate));
    }

    static inline float fastTanh(float x) noexcept {
        x = std::clamp(x, -3.0f, 3.0f);
        const float x2 = x * x;
        return x * (27.0f + x2) / (27.0f + 9.0f * x2);
    }

    void processBlock(float* buffer, int32_t numFrames) noexcept {
        float currentGain = mCurrentGain;
        const float targetGain = mTargetGain;
        const float alpha = mSmoothAlpha;
        const float drive = mDrive;
        const float norm = mNormFactor;

        for (int32_t i = 0; i < numFrames; ++i) {
            currentGain += alpha * (targetGain - currentGain);
            const float boosted = buffer[i] * currentGain;
            buffer[i] = fastTanh(boosted * drive) * norm;
        }
        mCurrentGain = currentGain;
    }

private:
    float mCurrentGain = 1.0f;
    float mTargetGain = 1.0f;
    float mDrive = 1.0f;
    float mNormFactor = 1.0f;
    float mSmoothAlpha = 0.01f;
};
