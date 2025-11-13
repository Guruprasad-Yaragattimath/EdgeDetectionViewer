#include <jni.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include "opencv_processing.h"

#define LOG_TAG "EDGECV"
#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_guru_edgedetectionviewer_camera_FrameProcessor_nativeProcessFrame(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray frameData,
        jint width,
        jint height) {

    if (frameData == nullptr) {
        ALOGE("frameData null");
        return nullptr;
    }

    jbyte *nv21 = env->GetByteArrayElements(frameData, nullptr);
    if (nv21 == nullptr) {
        ALOGE("GetByteArrayElements returned null");
        return nullptr;
    }

    cv::Mat yuv(height + height / 2, width, CV_8UC1,
                reinterpret_cast<unsigned char *>(nv21));

    cv::Mat processed = processFrame(yuv, width, height);

    if (processed.empty()) {
        ALOGE("processed empty");
        env->ReleaseByteArrayElements(frameData, nv21, JNI_ABORT);
        return nullptr;
    }

    if (!processed.isContinuous()) processed = processed.clone();

    if (processed.channels() == 3) {
        cv::Mat g;
        cv::cvtColor(processed, g, cv::COLOR_BGR2GRAY);
        processed = g;
    }

    int outSize = processed.rows * processed.cols;
    jbyteArray outArray = env->NewByteArray(outSize);
    if (outArray == nullptr) {
        ALOGE("Failed to allocate outArray");
        env->ReleaseByteArrayElements(frameData, nv21, JNI_ABORT);
        return nullptr;
    }

    env->SetByteArrayRegion(outArray, 0, outSize,
                            reinterpret_cast<jbyte *>(processed.data));

    env->ReleaseByteArrayElements(frameData, nv21, JNI_ABORT);

    return outArray;
}

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    ALOGD("JNI_OnLoad called");
    return JNI_VERSION_1_6;
}
