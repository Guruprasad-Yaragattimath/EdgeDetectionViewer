cmake_minimum_required(VERSION 3.22.1)

project(edgedetectionviewer2)

# -----------------------------------------------------------
# 1. Include OpenCV headers
# -----------------------------------------------------------
include_directories(${CMAKE_SOURCE_DIR}/include)

# -----------------------------------------------------------
# 2. Import the OpenCV shared library manually
# -----------------------------------------------------------
add_library(opencv_java4 SHARED IMPORTED)

set_target_properties(
        opencv_java4
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libopencv_java4.so
)

# -----------------------------------------------------------
# 3. Build your native code
# -----------------------------------------------------------
add_library(
        edgedetectionviewer2
        SHARED
        EdgeDetectionViewer2.cpp
)

# -----------------------------------------------------------
# 4. Link liblog from Android
# -----------------------------------------------------------
find_library(
        log-lib
        log
)

# -----------------------------------------------------------
# 5. Link everything together
# -----------------------------------------------------------
target_link_libraries(
        edgedetectionviewer2
        ${log-lib}
        opencv_java4
        c++_shared
)

