package com.guru.edgedetectionviewer.camera

object FrameProcessor {
    init {
        System.loadLibrary("native-lib")
    }

    external fun nativeProcessFrame(
        frameData: ByteArray,
        width: Int,
        height: Int
    ): ByteArray
}
