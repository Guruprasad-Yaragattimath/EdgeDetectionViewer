#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

// =======================================================
// stringFromJNI  → simple sanity check
// =======================================================
extern "C"
JNIEXPORT jstring JNICALL
Java_com_guru_edgedetectionviewer2_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    std::string msg = "JNI + OpenCV is ready!";
    return env->NewStringUTF(msg.c_str());
}

// =======================================================
// processFrameJNI  → count edges (for logging / testing)
// Input : Y plane (grayscale)
// Steps : Blur → Canny → count edge pixels
// Return: edge pixel count (int)
// =======================================================
extern "C"
JNIEXPORT jint JNICALL
Java_com_guru_edgedetectionviewer2_MainActivity_processFrameJNI(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray frameData,
        jint width,
        jint height
) {
    jbyte* dataPtr = env->GetByteArrayElements(frameData, nullptr);
    if (!dataPtr) {
        return 0;
    }

    int w = static_cast<int>(width);
    int h = static_cast<int>(height);

    cv::Mat yPlane(h, w, CV_8UC1,
                   reinterpret_cast<unsigned char*>(dataPtr));

    cv::Mat blurred;
    cv::GaussianBlur(yPlane, blurred, cv::Size(5, 5), 1.5);

    cv::Mat edges;
    cv::Canny(blurred, edges, 80, 150);

    int edgeCount = cv::countNonZero(edges);

    env->ReleaseByteArrayElements(frameData, dataPtr, JNI_ABORT);
    return edgeCount;
}

// =======================================================
// edgeToRGBA  → produce RGBA image from Y-plane edges
// Input : Y plane (grayscale) from camera (YUV_420_888)
// Steps : Blur → Canny → convert to RGBA
// Return: jbyteArray (width * height * 4) in RGBA order
// =======================================================
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_guru_edgedetectionviewer2_MainActivity_edgeToRGBA(
        JNIEnv* env,
        jobject /* this */ ,
        jbyteArray frameData,
        jint width,
        jint height
) {
    jbyte* dataPtr = env->GetByteArrayElements(frameData, nullptr);
    if (!dataPtr) {
        return nullptr;
    }

    int w = static_cast<int>(width);
    int h = static_cast<int>(height);

    // Wrap Y plane as grayscale Mat
    cv::Mat yPlane(h, w, CV_8UC1,
                   reinterpret_cast<unsigned char*>(dataPtr));

    // Blur + Canny
    cv::Mat blurred;
    cv::GaussianBlur(yPlane, blurred, cv::Size(5, 5), 1.5);

    cv::Mat edges;
    cv::Canny(blurred, edges, 80, 150);

    // Convert 1-channel edges → 4-channel RGBA
    cv::Mat rgba;
    cv::cvtColor(edges, rgba, cv::COLOR_GRAY2RGBA);

    // Allocate Java byte array
    const int outSize = w * h * 4;
    jbyteArray outArray = env->NewByteArray(outSize);
    if (!outArray) {
        env->ReleaseByteArrayElements(frameData, dataPtr, JNI_ABORT);
        return nullptr;
    }

    // Copy RGBA data into Java array
    env->SetByteArrayRegion(
            outArray,
            0,
            outSize,
            reinterpret_cast<jbyte*>(rgba.data)
    );

    env->ReleaseByteArrayElements(frameData, dataPtr, JNI_ABORT);
    return outArray;
}
