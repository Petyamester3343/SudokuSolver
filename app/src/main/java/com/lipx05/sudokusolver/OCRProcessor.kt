package com.lipx05.sudokusolver

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OCRProcessor(
    private val onSuccess: (String) -> Unit,
    private val onFailure: (String) -> Unit
) {
    private fun preprocessImg(bitmap: Bitmap?): Bitmap {
        val scaledBM = bitmap?.let { Bitmap.createScaledBitmap(it, 224, 224, true) }
        val processedBM = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(processedBM)
        val paint = Paint()

        val colorMat = ColorMatrix()
        colorMat.setSaturation(0f)
        val filter = ColorMatrixColorFilter(colorMat)
        paint.colorFilter = filter
        if (scaledBM != null) {
            canvas.drawBitmap(scaledBM, 0f, 0f, paint)
        }

        return processedBM
    }

    fun extractCells(gridBitmap: Bitmap): List<Bitmap> {
        val cellW = gridBitmap.width/9
        val cellH = gridBitmap.height/9
        val cells = mutableListOf<Bitmap>()

        for (i in 0 until 9) {
            for (j in 0 until 9) {
                recognizeDigit(gridBitmap)
                val x = j * cellW
                val y = i * cellH
                cells.add(Bitmap.createBitmap(gridBitmap, x, y, cellW, cellH))
            }
        }

        return cells
    }

    private fun recognizeDigit(bitmap: Bitmap?) {
        try {
            val processedBM = preprocessImg(bitmap)
            val inImg = InputImage.fromBitmap(processedBM, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inImg)
                .addOnSuccessListener { text ->
                    val recText = text.text
                    Log.d("OCR", "Recognized text: $recText")
                    onSuccess(recText)
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "Text recognition failed: ${e.message}")
                    onFailure("Text recognition failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("OCR", "Error during preprocessing or recognition: ${e.message}")
            onFailure("Error during preprocessing or recognition: ${e.message}")
        }
    }
}