package com.lipx05.sudokusolver

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
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

class CamManager(
    private val ctx: Context,
    private val previewView: PreviewView,
    private var ocrProcessor: OCRProcessor
) {
    private var imgCapture: ImageCapture? = null
    private val camExec: ExecutorService = Executors.newSingleThreadExecutor()
    private var isCamActive = false

    fun toggleCam(solveBtn: Button, captureBtn: Button, toggleCamBtn: Button, x: AppCompatActivity) {
        isCamActive = !isCamActive

        previewView.visibility = if(isCamActive) View.VISIBLE else View.GONE
        solveBtn.visibility = if(isCamActive) View.GONE else View.VISIBLE
        captureBtn.visibility = if(isCamActive) View.VISIBLE else View.GONE

        toggleCamBtn.text =
            if(!isCamActive) x.resources.getString(R.string.open_cam_str)
            else x.resources.getString(R.string.close_cam_str)
    }

    fun startCam() {
        val camProvFut = ProcessCameraProvider.getInstance(ctx)
        camProvFut.addListener({
            val camProv = camProvFut.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            imgCapture = ImageCapture.Builder().build()
            val camSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                camProv.unbindAll()
                camProv.bindToLifecycle(
                    (ctx as androidx.lifecycle.LifecycleOwner),
                    camSelector,
                    preview,
                    imgCapture
                )
            } catch(e: Exception) {
                Log.e("CameraX", "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    fun captureImg(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val capture = imgCapture ?: return

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
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImg4OCR(img: ImageProxy) {
        val mediaImg = img.image ?: return
        if(mediaImg.format == 0x100) {
            Toast.makeText(this.ctx, "JPEG", Toast.LENGTH_SHORT).show()
        }
        val conv = YuvToRgbConverter()
        val bitmap = conv.toBitmap(mediaImg)

        ocrProcessor = OCRProcessor(
            onSuccess = { recognizedText ->
                Log.d("OCR", "Recognized text: $recognizedText")
                Toast.makeText(this.ctx, recognizedText, Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                Log.e("OCR", error)
            }
        )
        ocrProcessor.extractCells(bitmap)
        img.close()
    }

    fun shutdown() {
        camExec.shutdown()
    }
}