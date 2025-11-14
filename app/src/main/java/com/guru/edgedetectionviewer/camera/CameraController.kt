package com.guru.edgedetectionviewer.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresPermission

class CameraController(
    private val context: Context,
    private val previewSurfaceTexture: SurfaceTexture?,
    private val onFrameAvailable: (ByteArray, Int, Int) -> Unit
) {

    private val TAG = "CameraController"

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var frameCounter = 0


    // ============================================================
    // START CAMERA
    // ============================================================
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    fun startCamera() {
        startBackgroundThread()

        val cameraId = chooseCameraId() ?: return
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val previewSize = choosePreviewSize(characteristics)

        Log.i(TAG, "Preview size = ${previewSize.width} x ${previewSize.height}")

        if (previewSurfaceTexture == null) {
            Log.e(TAG, "SurfaceTexture is NULL — cannot start camera!")
            return
        }

        previewSurfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        previewSurface = Surface(previewSurfaceTexture)

        // Create ImageReader
        imageReader = ImageReader.newInstance(
            previewSize.width,
            previewSize.height,
            ImageFormat.YUV_420_888,
            2
        )

        // ******** KEY FIX: SPARSE LOGGING + FRAME VALIDATION ********
        imageReader!!.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            try {
                val nv21 = yuv420ToNV21(image)

                val expected = image.width * image.height * 3 / 2
                if (nv21.size != expected) {
                    Log.w(TAG, "NV21 invalid: got=${nv21.size} expected=$expected")
                }

                onFrameAvailable(nv21, image.width, image.height)

                frameCounter++
                if (frameCounter % 30 == 0) {
                    Log.i(TAG, "FRAME #$frameCounter delivered (${image.width}x${image.height})")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Frame error: ${e.message}")
            } finally {
                image.close()
            }
        }, backgroundHandler)

        // Open camera
        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    createSession(device)
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                }

                override fun onError(device: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error=$error")
                    device.close()
                }
            },
            backgroundHandler
        )
    }


    // ============================================================
    // CREATE SESSION
    // ============================================================
    private fun createSession(device: CameraDevice) {
        val targets = mutableListOf<Surface>()

        imageReader?.surface?.let { targets.add(it) }
        previewSurface?.let { targets.add(it) }

        device.createCaptureSession(
            targets,
            object : CameraCaptureSession.StateCallback() {

                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session

                    val builder =
                        device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                    imageReader?.surface?.let { builder.addTarget(it) }
                    previewSurface?.let { builder.addTarget(it) }

                    builder.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )

                    session.setRepeatingRequest(
                        builder.build(),
                        null,
                        backgroundHandler
                    )

                    Log.i(TAG, "Camera session configured successfully")
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Camera session FAILED")
                }
            },
            backgroundHandler
        )
    }


    // ============================================================
    // STOP CAMERA
    // ============================================================
    fun stopCamera() {
        try {
            captureSession?.close()
            cameraDevice?.close()
            imageReader?.close()
            previewSurfaceTexture?.release()
        } catch (_: Exception) { }

        stopBackgroundThread()
    }


    // ============================================================
    // BACKGROUND THREADS
    // ============================================================
    private fun startBackgroundThread() {
        if (backgroundThread != null) return

        backgroundThread = HandlerThread("CameraThread").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
        Log.d(TAG, "Background thread started")
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }


    // ============================================================
    // CAMERA CHOICE
    // ============================================================
    private fun chooseCameraId(): String? {
        for (id in cameraManager.cameraIdList) {
            val chars = cameraManager.getCameraCharacteristics(id)
            if (chars.get(CameraCharacteristics.LENS_FACING)
                == CameraCharacteristics.LENS_FACING_BACK
            ) return id
        }
        return cameraManager.cameraIdList.firstOrNull()
    }

    private fun choosePreviewSize(chars: CameraCharacteristics): Size {
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val sizes = map.getOutputSizes(ImageFormat.YUV_420_888)

        return sizes.firstOrNull { it.width == 1280 && it.height == 720 }
            ?: sizes.firstOrNull { it.width == 640 && it.height == 480 }
            ?: sizes[0]
    }


    // ============================================================
    // YUV → NV21
    // ============================================================
    private fun yuv420ToNV21(image: Image): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = yPlane.buffer.remaining()
        val nv21 = ByteArray(ySize + image.width * image.height / 2)

        yPlane.buffer.get(nv21, 0, ySize)

        val pixelStride = uPlane.pixelStride
        val rowStride = uPlane.rowStride
        val uvWidth = image.width / 2
        val uvHeight = image.height / 2

        var offset = ySize

        for (row in 0 until uvHeight) {
            val rowStart = row * rowStride
            for (col in 0 until uvWidth) {
                val index = rowStart + col * pixelStride
                nv21[offset++] = vPlane.buffer[index]
                nv21[offset++] = uPlane.buffer[index]
            }
        }

        return nv21
    }
}
