package com.guru.edgedetectionviewer.camera

object FrameProcessor {

    init {
        // Must match CMake add_library(<name>)
        System.loadLibrary("native-lib")
    }

    external fun nativeProcessFrame(
        frameData: ByteArray,
        width: Int,
        height: Int
    ): ByteArray
}
