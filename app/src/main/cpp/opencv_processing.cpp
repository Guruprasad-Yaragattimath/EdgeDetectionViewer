#include "opencv_processing.h"

cv::Mat processFrame(const cv::Mat &yuv, int width, int height) {
    // Convert NV21 (YUV) to BGR
    cv::Mat bgr;
    cv::cvtColor(yuv, bgr, cv::COLOR_YUV2BGR_NV21);

    // Convert to gray
    cv::Mat gray;
    cv::cvtColor(bgr, gray, cv::COLOR_BGR2GRAY);

    // Canny edges
    cv::Mat edges;
    cv::Canny(gray, edges, 80, 150);

    return edges; // single-channel CV_8U
}
