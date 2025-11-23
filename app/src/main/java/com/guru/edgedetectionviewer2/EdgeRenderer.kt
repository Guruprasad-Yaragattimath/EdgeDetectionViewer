package com.guru.edgedetectionviewer2

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class EdgeRenderer : GLSurfaceView.Renderer {

    // OpenGL handles
    private var program = 0
    private var textureId = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var samplerHandle = 0

    // Frame data (RGBA from JNI)
    private var frameBuffer: ByteBuffer? = null
    private var frameWidth: Int = 0
    private var frameHeight: Int = 0

    // Full-screen quad vertices: (x, y, u, v)
    private val vertexData = floatArrayOf(
        //  X,   Y,   U,  V
        -1f, -1f,  0f, 1f,   // bottom-left
        1f, -1f,  1f, 1f,   // bottom-right
        -1f,  1f,  0f, 0f,   // top-left
        1f,  1f,  1f, 0f    // top-right
    )

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertexData)
                position(0)
            }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        GLES20.glUseProgram(program)

        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        samplerHandle = GLES20.glGetUniformLocation(program, "uTexture")

        // Create texture
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        textureId = texIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val buf = frameBuffer ?: return
        if (frameWidth == 0 || frameHeight == 0) return

        GLES20.glUseProgram(program)

        // Bind texture and upload latest frame
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        buf.position(0)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            frameWidth,
            frameHeight,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            buf
        )

        // Enable attributes
        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            4 * 4,
            vertexBuffer
        )

        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(
            texCoordHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            4 * 4,
            vertexBuffer
        )

        // Use texture unit 0
        GLES20.glUniform1i(samplerHandle, 0)

        // Draw full-screen quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    /**
     * Called from MainActivity (camera thread) with RGBA data from JNI.
     */
    fun setFrame(data: ByteArray, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        val needed = width * height * 4
        if (frameBuffer == null || frameBuffer!!.capacity() != needed) {
            frameBuffer = ByteBuffer
                .allocateDirect(needed)
                .order(ByteOrder.nativeOrder())
        }

        frameWidth = width
        frameHeight = height

        val buf = frameBuffer ?: return
        buf.position(0)
        buf.put(data)
        buf.position(0)
    }

    companion object {

        private const val TAG = "EdgeRenderer"

        // Vertex shader flips V so the texture isn’t upside-down.
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = vec2(aTexCoord.x, 1.0 - aTexCoord.y);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """

        private fun loadShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)

            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader $type:")
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader))
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader compile failed")
            }
            return shader
        }

        private fun createProgram(vs: String, fs: String): Int {
            val vShader = loadShader(GLES20.GL_VERTEX_SHADER, vs)
            val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fs)

            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vShader)
            GLES20.glAttachShader(program, fShader)
            GLES20.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Could not link program:")
                Log.e(TAG, GLES20.glGetProgramInfoLog(program))
                GLES20.glDeleteProgram(program)
                throw RuntimeException("Program link failed")
            }
            return program
        }
    }
}
