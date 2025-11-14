package com.guru.edgedetectionviewer.camera

import android.Manifest
<<<<<<< HEAD
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
=======
import android.content.Context
import android.graphics.ImageFormat
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
<<<<<<< HEAD
import android.util.Log
import android.util.Size
import android.view.Surface
=======
import android.util.Size
import android.view.Surface
import android.view.TextureView
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
import androidx.annotation.RequiresPermission

class CameraController(
    private val context: Context,
<<<<<<< HEAD
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
=======
    private val textureView: TextureView,
    private val onFrameAvailable: (ByteArray, Int, Int) -> Unit
) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

<<<<<<< HEAD
    private var frameCounter = 0


    // ============================================================
    // START CAMERA
    // ============================================================
    @SuppressLint("MissingPermission")
=======
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
    @RequiresPermission(Manifest.permission.CAMERA)
    fun startCamera() {
        startBackgroundThread()

<<<<<<< HEAD
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
=======
        val cameraId = cameraManager.cameraIdList[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        val previewSize = choosePreviewSize(characteristics)

>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
        imageReader = ImageReader.newInstance(
            previewSize.width,
            previewSize.height,
            ImageFormat.YUV_420_888,
            2
        )

<<<<<<< HEAD
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
=======
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val nv21 = yuv420ToNV21(image)
                onFrameAvailable(nv21, image.width, image.height)
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
                image.close()
            }
        }, backgroundHandler)

<<<<<<< HEAD
        // Open camera
=======
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
<<<<<<< HEAD
                    createSession(device)
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                }

                override fun onError(device: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error=$error")
                    device.close()
                }
=======
                    createPreviewSession(previewSize)
                }

                override fun onDisconnected(device: CameraDevice) {}
                override fun onError(device: CameraDevice, error: Int) {}
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
            },
            backgroundHandler
        )
    }

<<<<<<< HEAD

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
=======
    private fun createPreviewSession(size: Size) {
        val texture = textureView.surfaceTexture!!
        texture.setDefaultBufferSize(size.width, size.height)

        val previewSurface = Surface(texture)
        val surfaces = listOf(previewSurface, imageReader.surface)

        cameraDevice.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session

                    val builder = cameraDevice.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW
                    )

                    builder.addTarget(previewSurface)
                    builder.addTarget(imageReader.surface)
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9

                    builder.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )

                    session.setRepeatingRequest(
                        builder.build(),
                        null,
                        backgroundHandler
                    )
<<<<<<< HEAD

                    Log.i(TAG, "Camera session configured successfully")
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Camera session FAILED")
                }
=======
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
            },
            backgroundHandler
        )
    }

<<<<<<< HEAD

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
=======
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraThread").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    fun stopCamera() {
        captureSession.close()
        cameraDevice.close()
        backgroundThread?.quitSafely()
    }

    private fun choosePreviewSize(chars: CameraCharacteristics): Size {
        val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return map!!.getOutputSizes(ImageFormat.YUV_420_888)[0]
    }

    // ------------------------------
    //  NV21 Conversion Function
    // ------------------------------

>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
    private fun yuv420ToNV21(image: Image): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

<<<<<<< HEAD
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
=======
        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copy Y
        yBuffer.get(nv21, 0, ySize)

        // NV21 = Y + VU interleaved
        val width = image.width
        val height = image.height

        val rowStride = uPlane.rowStride
        val pixelStride = uPlane.pixelStride

        var outputPos = ySize
        val uvHeight = height / 2

        for (row in 0 until uvHeight) {
            val uvRowStart = row * rowStride
            for (col in 0 until width / 2) {
                val offset = uvRowStart + col * pixelStride
                nv21[outputPos++] = vBuffer.get(offset) // V
                nv21[outputPos++] = uBuffer.get(offset) // U
>>>>>>> 85938770f3fe26b96fb447b109a567e68e88ffc9
            }
        }

        return nv21
    }
}
