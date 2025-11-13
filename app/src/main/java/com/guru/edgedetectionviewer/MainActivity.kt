package com.guru.edgedetectionviewer

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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraController.stopCamera()
    }
}
