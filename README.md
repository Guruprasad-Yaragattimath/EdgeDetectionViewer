📸 EdgeDetectionViewer

Android + OpenCV + C++ + OpenGL ES (Real-Time Edge Detection Viewer)

A high-performance Android application that captures camera frames using Camera2 API, processes them with OpenCV (C++ native), and renders the processed output using OpenGL ES.

🚀 Features
✅ Camera2 API

High-resolution camera preview

YUV → NV21 frame extraction

Background-thread image acquisition

Stable, low-latency frame pipeline

🧠 C++ / OpenCV Processing

Native JNI bridge

Real-time edge detection (Sobel / Canny)

Efficient memory handling

Customizable OpenCV pipeline in opencv_processing.cpp

🎨 OpenGL ES Renderer

GLSurfaceView-based rendering

Displays processed grayscale textures

Fast rendering with shaders

Smooth 60 FPS pipeline (device dependent)
