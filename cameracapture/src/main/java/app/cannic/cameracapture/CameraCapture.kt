package app.cannic.cameracapture

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import app.cannic.cameracapture.data.ImageProxyMetadata
import app.cannic.cameracapture.data.ImageProxyWithMetadata
import java.util.concurrent.LinkedBlockingQueue


/**
 * A class the captures images using the camera and associate them with camera's metadata.
 *
 * @property context The [Context] on which the capture will be executed.
 * @property lifecycleOwner The [LifecycleOwner] which controls the lifecycle transitions of the use cases.
 * @property onNewImage A callback invoked when a new [ImageProxyWithMetadata] is available.
 */
class CameraCapture(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    val onNewImage: (ImageProxyWithMetadata) -> Unit
) {
    private var cameraProvider: ProcessCameraProvider? = null

    fun startCapture() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val imageAnalysis = buildImageAnalysis(
                context = context,
                onImageWithMetadataCreated = { imageProxyWithMetadata ->
                    onNewImage(imageProxyWithMetadata)
                }
            )
            try {
                with(cameraProviderFuture.get()) {
                    cameraProvider = this
                    this.unbindAll()
                    this.bindToLifecycle(
                        lifecycleOwner = lifecycleOwner,
                        cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build(),
                        imageAnalysis
                    )
                }
            } catch (e: Exception) {
                Log.e("CameraCapture", "Failed to bind camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCapture() {
        cameraProvider?.unbindAll()
    }

    /**
     * Listen for camera metadata and captured preview images, then matches and returns the results
     * through the [onImageWithMetadataCreated] callback.
     *
     * The metadata and the preview images are captured through two different callbacks from both
     * the Camera2 and CameraX APIs. These callbacks are triggered after each preview capture and
     * returns a identical timestamp that will be used to match both the metadata and the image.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun buildImageAnalysis(
        context: Context,
        onImageWithMetadataCreated: (ImageProxyWithMetadata) -> Unit
    ): ImageAnalysis {
        // Queues for capture results and images waiting to be matched.
        val resultQueue: LinkedBlockingQueue<CaptureResult> = LinkedBlockingQueue()
        val imageQueue: LinkedBlockingQueue<ImageProxy> = LinkedBlockingQueue()

        val builder = ImageAnalysis.Builder()
        val camera2InterOp = Camera2Interop.Extender(builder)
        // Listen to camera metadata.
        camera2InterOp.setSessionCaptureCallback(object :
            CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                resultQueue.offer(result)
                matchAndDispatchImageWithMetadata(
                    captureResultQueue = resultQueue,
                    imageQueue = imageQueue,
                    onImageWithMetadataCreated = onImageWithMetadataCreated
                )
            }
        })

        val imageAnalysis = builder.build()
        // Listen to captured images.
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
            imageQueue.offer(image)
            matchAndDispatchImageWithMetadata(
                captureResultQueue = resultQueue,
                imageQueue = imageQueue,
                onImageWithMetadataCreated = onImageWithMetadataCreated
            )
        }

        return imageAnalysis
    }

    private fun matchAndDispatchImageWithMetadata(
        captureResultQueue: LinkedBlockingQueue<CaptureResult>,
        imageQueue: LinkedBlockingQueue<ImageProxy>,
        onImageWithMetadataCreated: (ImageProxyWithMetadata) -> Unit
    ) {
        while (captureResultQueue.isNotEmpty() && imageQueue.isNotEmpty()) {
            val captureResult = captureResultQueue.peek()
            val image = imageQueue.peek()

            if (captureResult == null || image == null) return

            val captureResultTimestamp = captureResult.get(CaptureResult.SENSOR_TIMESTAMP) ?: -1L
            val imageTimestamp = image.imageInfo.timestamp

            when {
                captureResultTimestamp == imageTimestamp -> {
                    val metadata = ImageProxyMetadata(
                        iso = captureResult.get(CaptureResult.SENSOR_SENSITIVITY),
                        exposureTime = captureResult.get(CaptureResult.SENSOR_EXPOSURE_TIME)
                    )
                    val imageProxyWithMetadata = ImageProxyWithMetadata(
                        imageProxy = image,
                        metadata = metadata
                    )
                    onImageWithMetadataCreated(imageProxyWithMetadata)
                    captureResultQueue.poll()
                    imageQueue.poll()
                }
                captureResultTimestamp < imageTimestamp -> {
                    captureResultQueue.poll()
                }
                else -> {
                    imageQueue.poll()?.close()
                }
            }
        }
    }

}