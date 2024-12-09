package app.cannic.cameracapture.data

import androidx.camera.core.ImageProxy


/**
 * Associates an ImageProxy with camera metadata.
 *
 * @property imageProxy The [ImageProxy] representing the captured image.
 * @property metadata The [ImageProxyMetadata] associated to the ImageProxy, containing the ISO and
 * exposure time.
 */
data class ImageProxyWithMetadata(
    val imageProxy: ImageProxy,
    val metadata: ImageProxyMetadata
)

/**
 * Metadata associated to an ImageProxy.
 *
 * @property iso The ISO sensitivity of the camera when the image was captured.
 * @property exposureTime The exposure time in nanoseconds it took for the image to be captured.
 */
data class ImageProxyMetadata(
    val iso: Int?,
    val exposureTime: Long?
)