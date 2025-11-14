package com.guru.edgedetectionviewer

<<<<<<< HEAD
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.guru.edgedetectionviewer.camera.CameraController
import com.guru.edgedetectionviewer.camera.FrameProcessor
import com.guru.edgedetectionviewer.gl.GLView

class MainActivity : AppCompatActivity() {

    private lateinit var cameraPreview: TextureView
    private lateinit var glView: GLView
    private var cameraController: CameraController? = null

    private val CAMERA_REQ = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        cameraPreview = findViewById(R.id.cameraPreview)
        glView = findViewById(R.id.glView)

        if (!hasCameraPermission()) {
            requestCameraPermission()
        } else {
            setupTextureListener()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQ
        )
    }

    private fun setupTextureListener() {
        cameraPreview.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {

                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                    startCameraSafe(surface)
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            }
    }

    /** FIX: Wrap camera start in try/catch to handle SecurityException **/
    private fun startCameraSafe(surface: SurfaceTexture) {
        if (!hasCameraPermission()) {
            Log.e("MainActivity", "Camera permission not granted!")
            return
        }

        try {
            cameraController = CameraController(
                this,
                surface
            ) { nv21, width, height ->
                val processed = FrameProcessor.nativeProcessFrame(nv21, width, height)
                if (processed != null) {
                    glView.updateFrame(processed, width, height)
                }
            }

            cameraController?.startCamera()

        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException starting camera: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)  // ✔ FIXED

        if (requestCode == CAMERA_REQ &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupTextureListener()
=======
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import com.example.edgedetectionviewer.camera.CameraController
import com.example.edgedetectionviewer.camera.FrameProcessor
import com.guru.edgedetectionviewer.camera.CameraController
import com.guru.edgedetectionviewer.camera.FrameProcessor

class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var cameraController: CameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textureView = findViewById(R.id.textureView)

        textureView.post {
            cameraController = CameraController(
                this,
                textureView
            ) { nv21, width, height ->

                val processed =
                    FrameProcessor.nativeProcessFrame(nv21, width, height)

                // TODO: Send processed frame to OpenGL (Phase 5)
            }

            cameraController.startCamera()
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
        }
    }

    override fun onDestroy() {
        super.onDestroy()
<<<<<<< HEAD
        cameraController?.stopCamera()
=======
        cameraController.stopCamera()
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
    }
}
