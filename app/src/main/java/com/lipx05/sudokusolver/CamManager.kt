package com.lipx05.sudokusolver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CamManager(
    private val ctx: Context,
    private val previewView: PreviewView,
    private var ocrProcessor: OCRProcessor
) {

    private var imgCapture: ImageCapture? = null
    private val camExec: ExecutorService = Executors.newSingleThreadExecutor()
    private var isCamActive = false
    private var camProv: ProcessCameraProvider? = null

    fun toggleCam(
        solveBtn: Button,
        generateBtn: Button,
        captureBtn: Button,
        toggleCamBtn: Button,
        x: AppCompatActivity
    ) {
        isCamActive = !isCamActive

        previewView.visibility = if(isCamActive) View.VISIBLE else View.GONE
        generateBtn.visibility = if(isCamActive) View.GONE else View.VISIBLE
        solveBtn.visibility = if(isCamActive) View.GONE else View.VISIBLE
        captureBtn.visibility = if(isCamActive) View.VISIBLE else View.GONE

        toggleCamBtn.text =
            if(!isCamActive) x.resources.getString(R.string.open_cam_str)
            else x.resources.getString(R.string.close_cam_str)
    }

    fun startCam() {
        try {
            val camProvFut = ProcessCameraProvider.getInstance(ctx)
            camProvFut.addListener({
                try {
                    camProv = camProvFut.get(5, TimeUnit.SECONDS)
                    bindCameraUseCases()
                } catch (e: Exception) {
                    Log.e("CameraX", "Use case binding failed", e)
                    Handler(Looper.getMainLooper()).postDelayed({
                        startCam()
                    }, 1000)
                }
            }, ContextCompat.getMainExecutor(ctx))
        } catch(e: Exception) {
            Log.e("CameraX", "Camera start failed!", e)
        }
    }

    private fun bindCameraUseCases() {
        val provider = camProv ?: return

        try {
            val preview = Preview.Builder()
                .setTargetRotation(previewView.display.rotation)
                .build()
                .also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            imgCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.display.rotation)
                .build()

            val camSelector = CameraSelector.DEFAULT_BACK_CAMERA

            provider.unbindAll()
            provider.bindToLifecycle(
                (ctx as androidx.lifecycle.LifecycleOwner),
                camSelector,
                preview,
                imgCapture
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Use case binding failed!", e)
        }
    }

    fun captureImg(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val capture = imgCapture ?: run {
            onFailure("Camera not initialized!")
            return
        }

        try {
            capture.takePicture(
                ContextCompat.getMainExecutor(ctx),
                object: ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        processImg4OCR(image)
                        image.close()
                        onSuccess()
                    }

                    override fun onError(ex: ImageCaptureException) {
                        Log.e("CameraX", "Image capture failed: ${ex.message}", ex)
                        onFailure("Image capture failed: ${ex.message}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Camera capture failed", e)
            onFailure("Camera capture failed: ${e.message}")
        }
    }

    private fun enhanceBitmap(bitmap: Bitmap): Bitmap? {
        val enhancedBitmap = bitmap.config?.let {
            Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                it
            )
        }
        val paint = Paint()

        // Kontraszt és élesség növelése
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                1.5f, 0f, 0f, 0f, -25f,
                0f, 1.5f, 0f, 0f, -25f,
                0f, 0f, 1.5f, 0f, -25f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        enhancedBitmap?.let { Canvas(it) }?.drawBitmap(bitmap, 0f, 0f, paint)

        return enhancedBitmap
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImg4OCR(img: ImageProxy) {
        val mediaImg = img.image ?: return
        val conv = YuvToRgbConverter()
        val bitmap = conv.toBitmap(mediaImg)

        Log.d("OCR", "Original image size: ${bitmap.width}x${bitmap.height}")

        val enhancedBitmap = enhanceBitmap(bitmap)
        if (enhancedBitmap != null) {
            Log.d(
                "OCR",
                "Enhanced image size: ${enhancedBitmap.width}x${enhancedBitmap.height}"
            )
            ocrProcessor.extractCells(enhancedBitmap)
        } else {
            Log.e("OCR", "Failed to enhance image!");
        }
        img.close()
    }

    fun shutdown() {
        try {
            camProv?.unbindAll()
            camExec.shutdown()
            try {
                if(!camExec.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    camExec.shutdownNow()
                }
            } catch(e: Exception) {
                camExec.shutdownNow()
            }
        } catch (e: Exception) {
            Log.e("CameraX", "Shutdown failed", e)
        }
    }
}