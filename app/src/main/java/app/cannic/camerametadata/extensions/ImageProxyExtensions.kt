package app.cannic.camerametadata.extensions

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy


fun ImageProxy.toBitmapWithRotation(): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(this.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(this.toBitmap(), 0, 0, width, height, matrix, true)
}