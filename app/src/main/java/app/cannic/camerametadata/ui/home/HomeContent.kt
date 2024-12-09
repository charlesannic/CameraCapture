package app.cannic.camerametadata.ui.home

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.cannic.cameracapture.CameraCapture
import app.cannic.cameracapture.data.ImageProxyMetadata
import app.cannic.camerametadata.R
import app.cannic.camerametadata.extensions.toBitmapWithRotation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    var bitmapWithMetadata by remember {
        mutableStateOf<Pair<Bitmap, ImageProxyMetadata>?>(null)
    }
    var captureNextImage by remember {
        mutableStateOf(false)
    }

    if (cameraPermissionState.status.isGranted) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            remember {
                CameraCapture(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onNewImage = { imageProxyWithMetadata ->
                        if (captureNextImage) {
                            bitmapWithMetadata = Pair(
                                imageProxyWithMetadata.imageProxy.toBitmapWithRotation(),
                                imageProxyWithMetadata.metadata
                            )
                            captureNextImage = false
                        }
                        imageProxyWithMetadata.imageProxy.close()
                    }
                ).apply {
                    startCapture()
                }
            }

            bitmapWithMetadata?.let {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(16.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = getMetadataText(
                            context = context,
                            metadata = it.second
                        ),
                        color = Color.White
                    )
                }
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = it.first.asImageBitmap(),
                    contentDescription = stringResource(R.string.capturedImageContentDescription)
                )
            }
            Button(
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeContent)
                    .align(Alignment.BottomCenter),
                onClick = {
                    captureNextImage = true
                }
            ) {
                Text(
                    text = stringResource(R.string.showNextCapturedImage)
                )
            }
        }
    } else {
        LaunchedEffect(cameraPermissionState) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
}

fun getMetadataText(context: Context, metadata: ImageProxyMetadata): String {
    return "${context.getString(R.string.iso)}: ${metadata.iso}" +
            "\n${context.getString(R.string.shutterSpeed)}: ${metadata.exposureTime}"
}