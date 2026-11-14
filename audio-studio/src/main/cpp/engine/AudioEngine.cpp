#include "AudioEngine.h"
#include <android/log.h>
#include <cmath>
#include <algorithm>
#include <unistd.h>
#include <sched.h>

#define TAG "AudioStudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

AudioEngine::AudioEngine() {
    mBooster.setParameters(6.0f, 1.2f, 48000.0f);
    mCompressor.prepare(48000.0f);
    mCompressor.setParameters(-18.0f, 4.0f, 6.0f, 5.0f, 80.0f, 3.0f, true);
    mEqualizer.prepare(48000.0f);
    mEqualizer.setGains(0.0f, 0.0f, 0.0f);
    mReverb.prepare(48000.0f);
    mReverb.setParameters(0.5f, 0.2f, 0.0f);
}

AudioEngine::~AudioEngine() {
    stop();
}

void AudioEngine::configureRealtimeThread() {
    // 1. Flush denormals to zero on ARM64 to eliminate 100x CPU spikes in IIR biquad filters
#if defined(__aarch64__)
    uint64_t fpcr;
    __asm__ __volatile__("mrs %0, fpcr" : "=r"(fpcr));
    __asm__ __volatile__("msr fpcr, %0" : : "r"(fpcr | (1ULL << 24))); // Set FZ bit
#endif

    // 2. Pin to big cores (typically cores 4..7 on ARM big.LITTLE architectures)
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    for (int cpu = 4; cpu < 8; ++cpu) {
        CPU_SET(cpu, &cpuset);
    }
    sched_setaffinity(gettid(), sizeof(cpu_set_t), &cpuset);
}

bool AudioEngine::start(bool speakerModeAec) {
    if (mIsRunning.load()) return true;

    mCurrentParams.speakerSafeAec = speakerModeAec;
    mDrainCount = 3;
    mThreadConfigured.store(false);

    // 1. Configure and open Output Stream (Master Clock)
    oboe::AudioStreamBuilder outBuilder;
    outBuilder.setDirection(oboe::Direction::Output)
              ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
              ->setSharingMode(oboe::SharingMode::Exclusive)
              ->setFormat(oboe::AudioFormat::Float)
              ->setChannelCount(oboe::ChannelCount::Stereo)
              ->setUsage(speakerModeAec ? oboe::Usage::VoiceCommunication : oboe::Usage::Media)
              ->setDataCallback(this)
              ->setErrorCallback(this);

    oboe::Result result = outBuilder.openStream(mPlayStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open play stream: %s", oboe::convertToText(result));
        return false;
    }

    mFramesPerBurst = mPlayStream->getFramesPerBurst();
    mPlayStream->setBufferSizeInFrames(mFramesPerBurst * 2); // Double buffering
    const int32_t sampleRate = mPlayStream->getSampleRate();

    // Prepare DSP filters with actual native hardware sample rate
    mBooster.setParameters(mCurrentParams.boostDb, 1.2f, static_cast<float>(sampleRate));
    mCompressor.prepare(static_cast<float>(sampleRate));
    mEqualizer.prepare(static_cast<float>(sampleRate));
    mReverb.prepare(static_cast<float>(sampleRate));

    // 2. Configure and open Input Stream matching Output Stream's native sample rate
    oboe::AudioStreamBuilder inBuilder;
    inBuilder.setDirection(oboe::Direction::Input)
             ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
             ->setSharingMode(oboe::SharingMode::Exclusive)
             ->setFormat(oboe::AudioFormat::Float)
             ->setChannelCount(oboe::ChannelCount::Mono)
             ->setSampleRate(sampleRate)
             ->setInputPreset(speakerModeAec ? oboe::InputPreset::VoiceCommunication
                                             : oboe::InputPreset::Unprocessed)
             ->setBufferCapacityInFrames(mFramesPerBurst * 4);

    result = inBuilder.openStream(mRecordStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open record stream: %s", oboe::convertToText(result));
        mPlayStream->close();
        mPlayStream.reset();
        return false;
    }

    mInputBuffer.resize(static_cast<size_t>(mPlayStream->getBufferCapacityInFrames()));

    if (mRecordStream->requestStart() != oboe::Result::OK) {
        LOGE("Failed to start record stream");
        stop();
        return false;
    }

    if (mPlayStream->requestStart() != oboe::Result::OK) {
        LOGE("Failed to start play stream");
        stop();
        return false;
    }

    mIsRunning.store(true);
    LOGI("Audio Engine started successfully at %d Hz, burst = %d frames", sampleRate, mFramesPerBurst);
    return true;
}

void AudioEngine::stop() {
    mIsRunning.store(false);

    if (mPlayStream) {
        mPlayStream->stop();
        mPlayStream->close();
        mPlayStream.reset();
    }
    if (mRecordStream) {
        mRecordStream->stop();
        mRecordStream->close();
        mRecordStream.reset();
    }
    mInPeak.store(0.0f);
    mInRms.store(0.0f);
    mOutPeak.store(0.0f);
    mOutRms.store(0.0f);
}

void AudioEngine::updateParams(const DspParams& params) {
    mParamQueue.tryPush(params);
}

void AudioEngine::processDsp(float* buffer, int32_t numFrames) {
    // 1. Preamp Boost with Padé Tanh Saturation
    mBooster.processBlock(buffer, numFrames);

    // 2. Studio Dynamic Compressor
    mCompressor.processBlock(buffer, numFrames);

    // 3. 3-Band Tone Equalizer
    mEqualizer.processBlock(buffer, numFrames);

    // 4. Algorithmic Reverb
    mReverb.processBlock(buffer, numFrames);

    // 5. Output Limiter / Soft-clipping (protects DAC and speakers from wrap-around)
    for (int32_t i = 0; i < numFrames; ++i) {
        buffer[i] = SoftClipperBooster::fastTanh(buffer[i]);
    }
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream* outStream,
                                                   void* audioData,
                                                   int32_t numFrames) {
    if (!mThreadConfigured.exchange(true, std::memory_order_relaxed)) {
        configureRealtimeThread();
    }

    // Drain initial record bursts to allow hardware pipelines to stabilize
    if (mDrainCount > 0) {
        int32_t totalDrained = 0;
        do {
            auto res = mRecordStream->read(mInputBuffer.data(), numFrames, 0);
            if (!res || res.value() <= 0) break;
            totalDrained += res.value();
        } while (totalDrained < numFrames * 4);
        mDrainCount--;
    }

    // Read input frames with 0-timeout (non-blocking)
    auto readResult = mRecordStream->read(mInputBuffer.data(), numFrames, 0);
    int32_t framesRead = readResult ? readResult.value() : 0;

    // Pad with silence if underrun on input
    if (framesRead < numFrames) {
        std::fill(mInputBuffer.begin() + framesRead, mInputBuffer.begin() + numFrames, 0.0f);
    }

    // Drain any pending parameter updates from UI
    DspParams updated;
    while (mParamQueue.tryPop(updated)) {
        mCurrentParams = updated;
        mBooster.setParameters(mCurrentParams.boostDb, 1.2f, static_cast<float>(outStream->getSampleRate()));
        mCompressor.setParameters(mCurrentParams.compThresholdDb,
                                  mCurrentParams.compRatio,
                                  6.0f, 5.0f, 80.0f,
                                  mCurrentParams.compMakeupDb, true);
        mEqualizer.setGains(mCurrentParams.toneLowDb,
                            mCurrentParams.toneMidDb,
                            mCurrentParams.toneHighDb);
        mReverb.setParameters(mCurrentParams.reverbSize, 0.25f, mCurrentParams.reverbWet);
    }

    // Calculate Input Peak & RMS
    float inPeak = 0.0f;
    float inSumSq = 0.0f;
    for (int32_t i = 0; i < numFrames; ++i) {
        const float val = std::fabs(mInputBuffer[i]);
        if (val > inPeak) inPeak = val;
        inSumSq += val * val;
    }
    mInPeak.store(inPeak, std::memory_order_relaxed);
    mInRms.store(std::sqrt(inSumSq / static_cast<float>(numFrames)), std::memory_order_relaxed);

    float* outStereo = static_cast<float*>(audioData);

    if (mCurrentParams.liveMonitorEnabled) {
        // Run real-time DSP
        processDsp(mInputBuffer.data(), numFrames);

        // Calculate Output Peak & RMS and write stereo interleaved
        float outPeak = 0.0f;
        float outSumSq = 0.0f;

        for (int32_t i = 0; i < numFrames; ++i) {
            const float sample = mInputBuffer[i];
            const float absSample = std::fabs(sample);
            if (absSample > outPeak) outPeak = absSample;
            outSumSq += sample * sample;

            // Stereo output (L and R)
            outStereo[i * 2] = sample;
            outStereo[i * 2 + 1] = sample;
        }

        mOutPeak.store(outPeak, std::memory_order_relaxed);
        mOutRms.store(std::sqrt(outSumSq / static_cast<float>(numFrames)), std::memory_order_relaxed);
    } else {
        // Muted output
        std::fill(outStereo, outStereo + (numFrames * 2), 0.0f);
        mOutPeak.store(0.0f, std::memory_order_relaxed);
        mOutRms.store(0.0f, std::memory_order_relaxed);
    }

    // Dynamic XRun adaptation: step up buffer size if glitches occur
    auto xRunResult = outStream->getXRunCount();
    const int32_t currentXRuns = xRunResult ? xRunResult.value() : 0;
    if (currentXRuns > mLastXRunCount) {
        mLastXRunCount = currentXRuns;
        const int32_t currentSize = outStream->getBufferSizeInFrames();
        const int32_t maxTarget = mFramesPerBurst * 4;
        if (currentSize < maxTarget) {
            outStream->setBufferSizeInFrames(currentSize + mFramesPerBurst);
        }
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    LOGE("AudioEngine stream closed with error: %s", oboe::convertToText(error));
}
