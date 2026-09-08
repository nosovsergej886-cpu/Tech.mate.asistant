package com.example.ui.components

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File

class CameraController(
    val takePhoto: (onSuccess: (Uri) -> Unit, onError: (Exception) -> Unit) -> Unit
)

@Composable
fun CameraViewfinder(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false,
    isFlashOn: Boolean = false,
    onControllerReady: (CameraController) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val cameraSelector = remember(isFrontCamera) {
        if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }

    LaunchedEffect(imageCapture) {
        val capture = imageCapture ?: return@LaunchedEffect
        val controller = CameraController(
            takePhoto = { onSuccess, onError ->
                try {
                    val photoFile = File(context.cacheDir, "techmate_cam_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onSuccess(Uri.fromFile(photoFile))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("CameraViewfinder", "Capture error: ${exception.message}", exception)
                                onError(exception)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("CameraViewfinder", "Exception during takePhoto: ${e.message}", e)
                    onError(e)
                }
            }
        )
        onControllerReady(controller)
    }

    // Rebind camera cleanly when viewfinder, selector, or flash changes
    LaunchedEffect(previewViewRef, cameraSelector, isFlashOn) {
        val pView = previewViewRef ?: return@LaunchedEffect
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProviderRef = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(pView.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setFlashMode(if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                    .build()
                imageCapture = capture

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
            } catch (e: Exception) {
                Log.e("CameraViewfinder", "Failed to bind camera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Safely unbind camera when Composable leaves the screen to prevent abandoned BufferQueue
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
                previewViewRef = null
            } catch (e: Exception) {
                Log.e("CameraViewfinder", "Failed to unbind camera on dispose: ${e.message}", e)
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    // Use COMPATIBLE (TextureView) to eliminate SurfaceView BLAST Consumer BufferQueue abandonment in Compose dialogs
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                }
            },
            update = { view ->
                if (previewViewRef != view) {
                    previewViewRef = view
                }
                imageCapture?.flashMode = if (isFlashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
            }
        )
    }
}
