#pragma once

#include <cmath>
#include <algorithm>
#include <cstdint>

class DynamicsProcessor {
public:
    void prepare(float sampleRate) {
        mSampleRate = sampleRate;
        updateCoefficients();
    }

    void setParameters(float thresholdDb, float ratio, float kneeWidthDb,
                       float attackMs, float releaseMs, float makeupDb, bool useRms) {
        mThresholdDb = thresholdDb;
        mRatio = std::max(1.0f, ratio);
        mKneeWidthDb = std::max(0.0f, kneeWidthDb);
        mAttackMs = std::max(0.05f, attackMs);
        mReleaseMs = std::max(1.0f, releaseMs);
        mMakeupDb = makeupDb;
        mUseRms = useRms;
        updateCoefficients();
    }

    void processBlock(float* buffer, int32_t numFrames) noexcept {
        for (int32_t i = 0; i < numFrames; ++i) {
            const float in = buffer[i];
            float detectorLinear = std::fabs(in);

            if (mUseRms) {
                mRmsState = mAlphaRms * mRmsState + (1.0f - mAlphaRms) * (in * in);
                detectorLinear = std::sqrt(mRmsState + 1.0e-12f);
            }

            // Convert to dB with -120 dBFS floor
            const float inputDb = 20.0f * std::log10(std::max(detectorLinear, 1.0e-6f));

            // Decoupled branching peak/RMS envelope follower in dB domain
            if (inputDb > mEnvDb) {
                mEnvDb = mAlphaAttack * mEnvDb + (1.0f - mAlphaAttack) * inputDb;
            } else {
                mEnvDb = mAlphaRelease * mEnvDb + (1.0f - mAlphaRelease) * inputDb;
            }

            // Soft-knee gain computer
            const float overshoot = mEnvDb - mThresholdDb;
            float gainReductionDb = 0.0f;

            if (2.0f * overshoot < -mKneeWidthDb) {
                gainReductionDb = 0.0f;
            } else if (2.0f * std::fabs(overshoot) <= mKneeWidthDb && mKneeWidthDb > 0.0f) {
                const float temp = overshoot + 0.5f * mKneeWidthDb;
                gainReductionDb = ((1.0f / mRatio - 1.0f) * temp * temp) / (2.0f * mKneeWidthDb);
            } else {
                gainReductionDb = overshoot * (1.0f / mRatio - 1.0f);
            }

            const float totalGainDb = gainReductionDb + mMakeupDb;
            const float linearGain = std::pow(10.0f, totalGainDb * 0.05f);
            buffer[i] = in * linearGain;
        }
    }

private:
    void updateCoefficients() {
        mAlphaAttack = std::exp(-1.0f / (0.001f * mAttackMs * mSampleRate));
        mAlphaRelease = std::exp(-1.0f / (0.001f * mReleaseMs * mSampleRate));
        mAlphaRms = std::exp(-1.0f / (0.010f * mSampleRate)); // 10 ms RMS window
    }

    float mSampleRate = 48000.0f;
    float mThresholdDb = -18.0f;
    float mRatio = 4.0f;
    float mKneeWidthDb = 6.0f;
    float mAttackMs = 5.0f;
    float mReleaseMs = 80.0f;
    float mMakeupDb = 0.0f;
    bool mUseRms = true;

    float mAlphaAttack = 0.99f;
    float mAlphaRelease = 0.999f;
    float mAlphaRms = 0.99f;
    float mRmsState = 0.0f;
    float mEnvDb = -120.0f;
};
