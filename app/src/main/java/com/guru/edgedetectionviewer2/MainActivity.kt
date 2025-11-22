package com.guru.edgedetectionviewer2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    // 🔹 JNI Functions
    external fun stringFromJNI(): String
    external fun processFrameJNI(frame: ByteArray, width: Int, height: Int): Int
    external fun edgeToRGBA(frame: ByteArray, width: Int, height: Int): ByteArray

    companion object {
        init {
            System.loadLibrary("edgedetectionviewer2")
        }
    }

    // 🔹 Camera Variables
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private val cameraId = "0"

    // ImageReader for YUV frames
    private lateinit var imageReader: ImageReader

    // OpenGL
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var glRenderer: EdgeRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Log.d("JNI_TEST", stringFromJNI())
        testJNIFrameProcessing()

        setupGLSurfaceView()
        checkCameraPermission()

        val textureView = findViewById<TextureView>(R.id.textureView)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                if (hasCameraPermission()) openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // ----------------------------------------------------------
    // OpenGL setup
    // ----------------------------------------------------------
    private fun setupGLSurfaceView() {
        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView.setEGLContextClientVersion(2)

        glRenderer = EdgeRenderer()
        glSurfaceView.setRenderer(glRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onResume() {
        super.onResume()
        if (::glSurfaceView.isInitialized) {
            glSurfaceView.onResume()
        }
    }

    override fun onPause() {
        if (::glSurfaceView.isInitialized) {
            glSurfaceView.onPause()
        }
        super.onPause()
    }

    // ----------------------------------------------------------
    // JNI test
    // ----------------------------------------------------------
    private fun testJNIFrameProcessing() {
        val w = 1280
        val h = 720
        val fakeFrame = ByteArray(w * h) { 1 }
        val result = processFrameJNI(fakeFrame, w, h)
        Log.d("JNI_FRAME_TEST", "JNI frame test successful → result = $result")
    }

    // ----------------------------------------------------------
    // Permissions
    // ----------------------------------------------------------
    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun checkCameraPermission() {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        if (code == 100 && results.isNotEmpty() &&
            results[0] == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        }
    }

    // ----------------------------------------------------------
    // Camera + ImageReader
    // ----------------------------------------------------------
    private fun openCamera() {
        if (!hasCameraPermission()) return
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) = camera.close()
                override fun onError(camera: CameraDevice, error: Int) = camera.close()

            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createPreviewSession() {

        val width = 1280
        val height = 720

        imageReader = ImageReader.newInstance(
            width, height, ImageFormat.YUV_420_888, 2
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            val yPlane = image.planes[0]
            val yBuffer: ByteBuffer = yPlane.buffer

            val yData = ByteArray(yBuffer.remaining())
            yBuffer.get(yData)

            image.close()

            // Count edges (still for log)
            val edgeCount = processFrameJNI(yData, width, height)
            Log.d("FRAME_JNI", "Processed frame → JNI output = $edgeCount")

            // Get RGBA edge image from C++
            val rgba = edgeToRGBA(yData, width, height)

            // Send RGBA frame to GL thread
            glSurfaceView.queueEvent {
                glRenderer.setFrame(rgba, width, height)
            }

        }, null)

        val textureView = findViewById<TextureView>(R.id.textureView)
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(width, height)

        val previewSurface = Surface(texture)
        val imageSurface = imageReader.surface

        captureRequestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)
        captureRequestBuilder.addTarget(imageSurface)

        cameraDevice.createCaptureSession(
            listOf(previewSurface, imageSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session

                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )

                    session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CAMERA", "Configuration failed")
                }
            },
            null
        )
    }
}
