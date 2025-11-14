package com.guru.edgedetectionviewer.gl

import android.content.Context
import android.util.AttributeSet
import android.opengl.GLSurfaceView

class GLView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer: MyRenderer

    init {
        // Must set BEFORE creating renderer
        setEGLContextClientVersion(2)

        renderer = MyRenderer(context)
        setRenderer(renderer)

        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateFrame(bytes: ByteArray, width: Int, height: Int) {
        renderer.updateFrame(bytes, width, height)
    }
}
