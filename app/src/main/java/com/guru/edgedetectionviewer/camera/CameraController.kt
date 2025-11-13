package com.guru.edgedetectionviewer.camera

import android.Manifest
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresPermission

class CameraController(
    private val context: Context,
    private val textureView: TextureView,
    private val onFrameAvailable: (ByteArray, Int, Int) -> Unit
) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    @RequiresPermission(Manifest.permission.CAMERA)
    fun startCamera() {
        startBackgroundThread()

        val cameraId = cameraManager.cameraIdList[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)

        val previewSize = choosePreviewSize(characteristics)

        imageReader = ImageReader.newInstance(
            previewSize.width,
            previewSize.height,
            ImageFormat.YUV_420_888,
            2
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                val nv21 = yuv420ToNV21(image)
                onFrameAvailable(nv21, image.width, image.height)
                image.close()
            }
        }, backgroundHandler)

        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    createPreviewSession(previewSize)
                }

                override fun onDisconnected(device: CameraDevice) {}
                override fun onError(device: CameraDevice, error: Int) {}
            },
            backgroundHandler
        )
    }

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

                    builder.set(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_AUTO
                    )

                    session.setRepeatingRequest(
                        builder.build(),
                        null,
                        backgroundHandler
                    )
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {}
            },
            backgroundHandler
        )
    }

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

    private fun yuv420ToNV21(image: Image): ByteArray {
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

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
            }
        }

        return nv21
    }
}
