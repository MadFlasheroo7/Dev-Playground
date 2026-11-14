#pragma once

#include <oboe/Oboe.h>
#include <atomic>
#include <memory>
#include <vector>
#include "common/LockFreeSpscQueue.h"
#include "dsp/SoftClipperBooster.h"
#include "dsp/DynamicsProcessor.h"
#include "dsp/BiquadFilter.h"
#include "dsp/Freeverb.h"

struct DspParams {
    float boostDb = 6.0f;
    float toneLowDb = 0.0f;
    float toneMidDb = 0.0f;
    float toneHighDb = 0.0f;
    float compThresholdDb = -18.0f;
    float compRatio = 4.0f;
    float compMakeupDb = 3.0f;
    float reverbSize = 0.5f;
    float reverbWet = 0.0f;
    bool liveMonitorEnabled = true;
    bool speakerSafeAec = false;
};

class AudioEngine : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    AudioEngine();
    ~AudioEngine() override;

    bool start(bool speakerModeAec);
    void stop();
    bool isRunning() const { return mIsRunning.load(); }

    void updateParams(const DspParams& params);

    // Lock-free level metering
    float getInputPeak() const { return mInPeak.load(std::memory_order_relaxed); }
    float getInputRms() const { return mInRms.load(std::memory_order_relaxed); }
    float getOutputPeak() const { return mOutPeak.load(std::memory_order_relaxed); }
    float getOutputRms() const { return mOutRms.load(std::memory_order_relaxed); }

    // Oboe Callbacks
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* outStream,
                                          void* audioData,
                                          int32_t numFrames) override;

    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    void configureRealtimeThread();
    void processDsp(float* buffer, int32_t numFrames);

    std::shared_ptr<oboe::AudioStream> mPlayStream;
    std::shared_ptr<oboe::AudioStream> mRecordStream;

    std::vector<float> mInputBuffer;
    std::atomic<bool> mIsRunning{false};
    std::atomic<bool> mThreadConfigured{false};

    int32_t mFramesPerBurst = 192;
    int32_t mLastXRunCount = 0;
    int32_t mDrainCount = 3;

    // Meters
    std::atomic<float> mInPeak{0.0f};
    std::atomic<float> mInRms{0.0f};
    std::atomic<float> mOutPeak{0.0f};
    std::atomic<float> mOutRms{0.0f};

    // SPSC Queue for parameter updates
    LockFreeSpscQueue<DspParams, 32> mParamQueue;
    DspParams mCurrentParams;

    // DSP components
    SoftClipperBooster mBooster;
    DynamicsProcessor mCompressor;
    ThreeBandEqualizer mEqualizer;
    Freeverb mReverb;
};
