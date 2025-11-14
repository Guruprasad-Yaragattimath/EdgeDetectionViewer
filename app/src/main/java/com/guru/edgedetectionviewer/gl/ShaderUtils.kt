package com.guru.edgedetectionviewer.gl

import android.opengl.GLES20
import android.util.Log

object ShaderUtils {

    private const val TAG = "ShaderUtils"

    // Compile a shader (vertex or fragment)
    fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Could not create shader of type $type")
            return 0
        }

        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            Log.e(TAG, "Shader compilation failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    // Link vertex + fragment shaders
    fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
        val program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Could not create GL program")
            return 0
        }

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program linking failed: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            return 0
        }

        return program
    }

    // Create a shader program from raw strings
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "Shader creation failed")
            return 0
        }

        return linkProgram(vertexShader, fragmentShader)
    }

    // Validate the program
    fun validateProgram(program: Int): Boolean {
        GLES20.glValidateProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_VALIDATE_STATUS, status, 0)

        Log.d(TAG, "Program validate status = ${status[0]}")
        Log.d(TAG, "Program validate log = ${GLES20.glGetProgramInfoLog(program)}")

        return status[0] != 0
    }
}
