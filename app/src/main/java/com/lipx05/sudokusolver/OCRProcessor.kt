package com.lipx05.sudokusolver

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OCRProcessor(
    private val onSuccess: (String) -> Unit,
    private val onFailure: (String) -> Unit
) {
    fun processImg(inImg: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inImg)
            .addOnSuccessListener { v ->
                onSuccess(v.text)
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Text recognition failed: ${e.message}", e)
                onFailure("Text recognition failed: ${e.message}")
            }
    }
}