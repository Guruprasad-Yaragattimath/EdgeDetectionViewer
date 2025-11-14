package com.guru.edgedetectionviewer.camera

object FrameProcessor {
<<<<<<< HEAD

    init {
        // Must match CMake add_library(<name>)
=======
    init {
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
        System.loadLibrary("native-lib")
    }

    external fun nativeProcessFrame(
        frameData: ByteArray,
        width: Int,
        height: Int
    ): ByteArray
}
