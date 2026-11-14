#include <jni.h>
#include "engine/AudioEngine.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeCreate(JNIEnv* env, jobject thiz) {
    return reinterpret_cast<jlong>(new AudioEngine());
}

JNIEXPORT void JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeDestroy(JNIEnv* env, jobject thiz, jlong enginePtr) {
    if (enginePtr != 0) {
        delete reinterpret_cast<AudioEngine*>(enginePtr);
    }
}

JNIEXPORT jboolean JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeStart(JNIEnv* env, jobject thiz, jlong enginePtr, jboolean speakerAec) {
    if (enginePtr == 0) return JNI_FALSE;
    AudioEngine* engine = reinterpret_cast<AudioEngine*>(enginePtr);
    return engine->start(speakerAec) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeStop(JNIEnv* env, jobject thiz, jlong enginePtr) {
    if (enginePtr != 0) {
        reinterpret_cast<AudioEngine*>(enginePtr)->stop();
    }
}

JNIEXPORT jboolean JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeIsRunning(JNIEnv* env, jobject thiz, jlong enginePtr) {
    if (enginePtr == 0) return JNI_FALSE;
    return reinterpret_cast<AudioEngine*>(enginePtr)->isRunning() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeUpdateParams(
        JNIEnv* env, jobject thiz, jlong enginePtr,
        jfloat boostDb, jfloat toneLowDb, jfloat toneMidDb, jfloat toneHighDb,
        jfloat compThreshDb, jfloat compRatio, jfloat compMakeupDb,
        jfloat reverbSize, jfloat reverbWet, jboolean liveMonitor, jboolean speakerAec) {
    if (enginePtr == 0) return;

    DspParams params;
    params.boostDb = boostDb;
    params.toneLowDb = toneLowDb;
    params.toneMidDb = toneMidDb;
    params.toneHighDb = toneHighDb;
    params.compThresholdDb = compThreshDb;
    params.compRatio = compRatio;
    params.compMakeupDb = compMakeupDb;
    params.reverbSize = reverbSize;
    params.reverbWet = reverbWet;
    params.liveMonitorEnabled = liveMonitor;
    params.speakerSafeAec = speakerAec;

    reinterpret_cast<AudioEngine*>(enginePtr)->updateParams(params);
}

JNIEXPORT jfloat JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeGetInputPeak(JNIEnv* env, jobject thiz, jlong enginePtr) {
    if (enginePtr == 0) return 0.0f;
    return reinterpret_cast<AudioEngine*>(enginePtr)->getInputPeak();
}

JNIEXPORT jfloat JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeGetInputRms(JNIEnv* env, jobject thiz, jlong enginePtr) {
    if (enginePtr == 0) return 0.0f;
    return reinterpret_cast<AudioEngine*>(enginePtr)->getInputRms();
}

JNIEXPORT jfloat JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeGetOutputPeak(JNIEnv* env, jobject thiz, jlong enginePtr) {
    if (enginePtr == 0) return 0.0f;
    return reinterpret_cast<AudioEngine*>(enginePtr)->getOutputPeak();
}

JNIEXPORT jfloat JNICALL
Java_pro_jayeshseth_audiostudio_NativeAudioEngine_nativeGetOutputRms(JNIEnv* env, jobject thiz, jlong enginePtr) {
    if (enginePtr == 0) return 0.0f;
    return reinterpret_cast<AudioEngine*>(enginePtr)->getOutputRms();
}

} // extern "C"
