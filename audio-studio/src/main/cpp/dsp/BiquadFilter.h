#pragma once

#include <cmath>
#include <algorithm>
#include <cstdint>

class BiquadFilter {
public:
    enum class Type {
        LowShelf,
        Peaking,
        HighShelf
    };

    void configure(Type type, float frequencyHz, float gainDb, float q, float sampleRate) {
        const float a = std::pow(10.0f, gainDb * 0.025f); // 10^(gainDb / 40)
        const float omega = 2.0f * M_PI * frequencyHz / sampleRate;
        const float sinOmega = std::sin(omega);
        const float cosOmega = std::cos(omega);
        const float alpha = sinOmega / (2.0f * q);
        const float beta = std::sqrt(a + a);

        float b0 = 1.0f, b1 = 0.0f, b2 = 0.0f, a0 = 1.0f, a1 = 0.0f, a2 = 0.0f;

        switch (type) {
            case Type::LowShelf: {
                b0 = a * ((a + 1.0f) - (a - 1.0f) * cosOmega + beta * sinOmega);
                b1 = 2.0f * a * ((a - 1.0f) - (a + 1.0f) * cosOmega);
                b2 = a * ((a + 1.0f) - (a - 1.0f) * cosOmega - beta * sinOmega);
                a0 = (a + 1.0f) + (a - 1.0f) * cosOmega + beta * sinOmega;
                a1 = -2.0f * ((a - 1.0f) + (a + 1.0f) * cosOmega);
                a2 = (a + 1.0f) + (a - 1.0f) * cosOmega - beta * sinOmega;
                break;
            }
            case Type::Peaking: {
                b0 = 1.0f + alpha * a;
                b1 = -2.0f * cosOmega;
                b2 = 1.0f - alpha * a;
                a0 = 1.0f + alpha / a;
                a1 = -2.0f * cosOmega;
                a2 = 1.0f - alpha / a;
                break;
            }
            case Type::HighShelf: {
                b0 = a * ((a + 1.0f) + (a - 1.0f) * cosOmega + beta * sinOmega);
                b1 = -2.0f * a * ((a - 1.0f) + (a + 1.0f) * cosOmega);
                b2 = a * ((a + 1.0f) + (a - 1.0f) * cosOmega - beta * sinOmega);
                a0 = (a + 1.0f) - (a - 1.0f) * cosOmega + beta * sinOmega;
                a1 = 2.0f * ((a - 1.0f) - (a + 1.0f) * cosOmega);
                a2 = (a + 1.0f) - (a - 1.0f) * cosOmega - beta * sinOmega;
                break;
            }
        }

        const float invA0 = 1.0f / a0;
        mB0 = b0 * invA0;
        mB1 = b1 * invA0;
        mB2 = b2 * invA0;
        mA1 = a1 * invA0;
        mA2 = a2 * invA0;
    }

    inline float processSample(float in) noexcept {
        // Direct Form II Transposed: minimal round-off noise, 2 state registers
        const float out = mB0 * in + mS1;
        mS1 = mB1 * in - mA1 * out + mS2;
        mS2 = mB2 * in - mA2 * out;
        return out;
    }

    void reset() noexcept {
        mS1 = 0.0f;
        mS2 = 0.0f;
    }

private:
    float mB0 = 1.0f, mB1 = 0.0f, mB2 = 0.0f;
    float mA1 = 0.0f, mA2 = 0.0f;
    float mS1 = 0.0f, mS2 = 0.0f;
};

class ThreeBandEqualizer {
public:
    void prepare(float sampleRate) {
        mSampleRate = sampleRate;
        updateFilters();
    }

    void setGains(float bassDb, float midDb, float trebleDb) {
        mBassDb = bassDb;
        mMidDb = midDb;
        mTrebleDb = trebleDb;
        updateFilters();
    }

    void processBlock(float* buffer, int32_t numFrames) noexcept {
        for (int32_t i = 0; i < numFrames; ++i) {
            float s = buffer[i];
            s = mLowShelf.processSample(s);
            s = mMidPeak.processSample(s);
            s = mHighShelf.processSample(s);
            buffer[i] = s;
        }
    }

private:
    void updateFilters() {
        mLowShelf.configure(BiquadFilter::Type::LowShelf, 220.0f, mBassDb, 0.707f, mSampleRate);
        mMidPeak.configure(BiquadFilter::Type::Peaking, 1600.0f, mMidDb, 1.0f, mSampleRate);
        mHighShelf.configure(BiquadFilter::Type::HighShelf, 5000.0f, mTrebleDb, 0.707f, mSampleRate);
    }

    float mSampleRate = 48000.0f;
    float mBassDb = 0.0f;
    float mMidDb = 0.0f;
    float mTrebleDb = 0.0f;

    BiquadFilter mLowShelf;
    BiquadFilter mMidPeak;
    BiquadFilter mHighShelf;
};
