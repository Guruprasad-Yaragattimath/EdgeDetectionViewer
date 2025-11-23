package com.guru.edgedetectionviewer2

import android.Manifest
import android.annotation.SuppressLint
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
import java.nio.ByteOrder

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class MainActivity : AppCompatActivity() {

    // ===== JNI =====
    external fun stringFromJNI(): String
    external fun processFrameJNI(frame: ByteArray, width: Int, height: Int): Int
    external fun edgeToRGBA(frame: ByteArray, width: Int, height: Int): ByteArray

    companion object {
        init {
            System.loadLibrary("edgedetectionviewer2")
        }
    }

    // ===== Reusable Upload Buffer =====
    private var uploadBuffer: ByteBuffer? = null
    private var uploadBufferSize = 0
    private var lastUpload = 0L   // FPS limiting

    // ===== Camera =====
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private val cameraId = "0"

    private lateinit var imageReader: ImageReader

    // ===== OpenGL =====
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var glRenderer: EdgeRenderer

    // ===== Network =====
    private val BASE_URL = "http://10.166.225.136:8080"
    private val UPLOAD_URL = "$BASE_URL/upload-frame"
    private val httpClient = OkHttpClient()

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
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    private fun setupGLSurfaceView() {
        glSurfaceView = findViewById(R.id.glSurfaceView)
        glSurfaceView.setEGLContextClientVersion(2)

        glRenderer = EdgeRenderer()
        glSurfaceView.setRenderer(glRenderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onResume() {
        super.onResume()
        if (::glSurfaceView.isInitialized) glSurfaceView.onResume()
    }

    override fun onPause() {
        if (::glSurfaceView.isInitialized) glSurfaceView.onPause()
        super.onPause()
    }

    private fun testJNIFrameProcessing() {
        val w = 1280
        val h = 720
        val fake = ByteArray(w * h) { 1 }
        Log.d("JNI_FRAME_TEST", "Result: ${processFrameJNI(fake, w, h)}")
    }

    // Permissions
    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun checkCameraPermission() {
        if (!hasCameraPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }

    // ============================
    // Camera Setup
    // ============================
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        if (!hasCameraPermission()) return
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(cam: CameraDevice) {
                cameraDevice = cam
                createPreviewSession()
            }
            override fun onDisconnected(cam: CameraDevice) = cam.close()
            override fun onError(cam: CameraDevice, err: Int) = cam.close()
        }, null)
    }

    private fun createPreviewSession() {
        val width = 1280
        val height = 720

        imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            val yPlane = image.planes[0]
            val buffer = yPlane.buffer
            val yData = ByteArray(buffer.remaining())
            buffer.get(yData)
            image.close()

            // JNI processing
            val rgba = edgeToRGBA(yData, width, height)

            // Render locally
            glSurfaceView.queueEvent {
                glRenderer.setFrame(rgba, width, height)
            }

            // Upload to server
            uploadFrameToServer(rgba, width, height)

        }, null)

        val textureView = findViewById<TextureView>(R.id.textureView)
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(width, height)

        val previewSurface = Surface(texture)
        val imageSurface = imageReader.surface

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)
        captureRequestBuilder.addTarget(imageSurface)

        cameraDevice.createCaptureSession(listOf(previewSurface, imageSurface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CAMERA", "Failed to configure")
                }
            }, null)
    }

    // ============================
    // Upload Frame to Node server
    // ============================
    private fun uploadFrameToServer(rgba: ByteArray, w: Int, h: Int) {

        // --- FPS LIMIT ---
        val now = System.currentTimeMillis()
        if (now - lastUpload < 100) return   // 10 FPS
        lastUpload = now
        // -----------------

        val needed = 8 + rgba.size

        // Reuse buffer
        if (uploadBuffer == null || uploadBufferSize != needed) {
            uploadBuffer = ByteBuffer.allocateDirect(needed).order(ByteOrder.LITTLE_ENDIAN)
            uploadBufferSize = needed
            Log.d("UPLOAD", "Allocated direct buffer of $needed bytes")
        }

        val buf = uploadBuffer!!
        buf.clear()

        buf.putInt(w)
        buf.putInt(h)
        buf.put(rgba)

        val body = RequestBody.create(
            "application/octet-stream".toMediaType(),
            buf.array(),
            0,
            needed
        )

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .post(body)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FRAME_UPLOAD", "Failed: ${e.localizedMessage}")
            }
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }
}
