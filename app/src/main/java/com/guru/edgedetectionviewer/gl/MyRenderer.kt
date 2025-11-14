package com.guru.edgedetectionviewer.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var program = 0
    private var textureId = 0

    private var frameBuffer: ByteArray? = null
    private var frameWidth = 0
    private var frameHeight = 0
    @Volatile private var frameUpdated = false

    // Save only one frame
    private var frameSavedOnce = false

    private val vertexData = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f,  1f,
        1f,  1f
    )

    private lateinit var vertexBuffer: FloatBuffer

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(vertexData).position(0)

        program = ShaderUtils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        GLES20.glUseProgram(program)

        val texLoc = GLES20.glGetUniformLocation(program, "uTexture")
        GLES20.glUniform1i(texLoc, 0)

        textureId = createTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (frameUpdated) {
            uploadTexture()

            // Save one processed frame (for Web Viewer)
            if (!frameSavedOnce && frameBuffer != null) {
                saveFrameToFile(frameBuffer!!, frameWidth, frameHeight)
                frameSavedOnce = true
            }

            frameUpdated = false
        }

        GLES20.glUseProgram(program)

        val posLoc = GLES20.glGetAttribLocation(program, "aPosition")
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    fun updateFrame(bytes: ByteArray, width: Int, height: Int) {
        frameBuffer = bytes
        frameWidth = width
        frameHeight = height
        frameUpdated = true
    }

    private fun uploadTexture() {
        val data = frameBuffer ?: return

        val buffer = ByteBuffer.allocateDirect(data.size)
        buffer.put(data).position(0)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0,
            GLES20.GL_LUMINANCE,
            frameWidth, frameHeight, 0,
            GLES20.GL_LUMINANCE,
            GLES20.GL_UNSIGNED_BYTE,
            buffer
        )
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val id = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        return id
    }

    // -----------------------------------
    // Save processed frame ONCE to app external files dir
    // -----------------------------------
    private fun saveFrameToFile(data: ByteArray, width: Int, height: Int) {
        try {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
            bmp.copyPixelsFromBuffer(ByteBuffer.wrap(data))

            val dir = context.getExternalFilesDir(null)
            val file = File(dir, "processed.png")

            FileOutputStream(file).use { stream ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            Log.i("MyRenderer", "Saved processed frame to ${file.absolutePath}")

        } catch (e: Exception) {
            Log.e("MyRenderer", "Error saving frame: ${e.message}")
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            varying vec2 vTexCoord;
            void main() {
                vTexCoord = (aPosition + 1.0) * 0.5;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                float gray = texture2D(uTexture, vTexCoord).r;
                gl_FragColor = vec4(gray, gray, gray, 1.0);
            }
        """
    }
}
