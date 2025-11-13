cmake_minimum_required(VERSION 3.10.2)

project("edgedetectionviewer")

# 1) Add the OpenCV headers you placed in app/src/main/cpp/opencv/include
include_directories(${CMAKE_SOURCE_DIR}/opencv/include)

# 2) Build your native library and include your actual .cpp files
add_library(
        native-lib
        SHARED
        edgedetectionviewer.cpp
        opencv_processing.cpp
)

# 3) Import OpenCV .so file
add_library(opencv_java4 SHARED IMPORTED)

set_target_properties(opencv_java4 PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so)

# 4) Link Android log + OpenCV
find_library(log-lib log)

target_link_libraries(
        native-lib
        opencv_java4
        ${log-lib}
)
