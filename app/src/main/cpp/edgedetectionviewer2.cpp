#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_guru_edgedetectionviewer2_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {

    // ✔ Create a synthetic grayscale 256x256 image
    cv::Mat img = cv::Mat::zeros(cv::Size(256, 256), CV_8UC1);

    // ✔ Draw a white rectangle (this is only for testing)
    cv::rectangle(img, cv::Point(50, 50), cv::Point(200, 200), cv::Scalar(255), 3);

    // ✔ Apply Canny Edge Detection
    cv::Mat edges;
    cv::Canny(img, edges, 80, 150);

    // ✔ Count number of edge pixels (just to test OpenCV)
    int edgeCount = cv::countNonZero(edges);

    std::string result = "OpenCV OK — EdgeCount = " + std::to_string(edgeCount);

    return env->NewStringUTF(result.c_str());
}
