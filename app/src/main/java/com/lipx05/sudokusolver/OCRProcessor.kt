package com.lipx05.sudokusolver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.Environment
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream

class OCRProcessor(
    private val ctx: Context,
    private val onSuccess: (String) -> Unit,
    private val onFailure: (String) -> Unit
) {
    private val recognizedGrid = Array(9) {IntArray(9)}
    private var processedCells = 0

    private fun preprocessImg(bitmap: Bitmap?): Bitmap {
        if(bitmap == null)
            return Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)

        val scaledBM = Bitmap.createScaledBitmap(
            bitmap,
            224,224,
            true
        )

        val processedBM = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(processedBM)
        val paint = Paint()

        val colorMat = ColorMatrix(floatArrayOf(
            2.0f, 0f, 0f, 0f, -30f,
            0f, 2.0f, 0f, 0f, -30f,
            0f, 0f, 2.0f, 0f, -30f,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMat)
        canvas.drawBitmap(scaledBM, 0f, 0f, paint)

        return processedBM
    }

    fun extractCells(gridBitmap: Bitmap): List<Bitmap> {
        processedCells = 0
        for(i in 0..8) {
            for (j in 0..8) {
                recognizedGrid[i][j] = 0
            }
        }

        Log.d("OCR", "Original bitmap size: ${gridBitmap.width}x${gridBitmap.height}")

        val tgtSize = 900
        val scaledBM = Bitmap.createScaledBitmap(
            gridBitmap,
            tgtSize,
            tgtSize,
            true
        )

        Log.d("OCR", "Scaled bitmap size: ${scaledBM.width}x${scaledBM.height}")

        val gridRect = findGridArea(scaledBM)
        Log.d("OCR", "Found grid area: $gridRect")

        val croppedGrid = Bitmap.createBitmap(
            gridBitmap,
            gridRect.left,
            gridRect.top,
            gridRect.width(),
            gridRect.height()
        )

        Log.d("OCR", "Cropped grid size: ${croppedGrid.width}x${croppedGrid.height}")

        val cellSize = croppedGrid.width / 9
        val cells = mutableListOf<Bitmap>()
        val padding = cellSize / 16

        for (i in 0 until 9) {
            for (j in 0 until 9) {
                try {
                    val x = j * cellSize + padding
                    val y = i * cellSize + padding
                    val size = cellSize - (2 * padding)

                    if(x + size <= croppedGrid.width
                        && y + size <= croppedGrid.height
                        && x >= 0 && y >= 0 && size > 0) {
                        val cell = Bitmap.createBitmap(
                            croppedGrid,
                            x, y, size, size
                        )
                        cells.add(cell)
                        recognizeDigit(cell, i ,j)
                    } else {
                        Log.e(
                            "OCR",
                            "Invalid cell dimensions at ($i, $j): x=$x, y=$y, size=$size"
                        )
                        processedCells++
                    }
                } catch(e: Exception) {
                    Log.e("OCR", "Error extracting cell at ($i, $j): ${e.message}")
                    processedCells++
                }
            }
        }

        // Debug: Felismert számok
        Log.d("OCR", "Recognized grid:")
        for (i in 0..8) {
            Log.d("OCR", recognizedGrid[i].joinToString(" "))
        }

        return cells
    }

    private fun findGridArea(bitmap: Bitmap): Rect {
        // Feladvány keresése (fehér)
        var top = 0
        var bottom = bitmap.height
        var left = 0
        var right = bitmap.width

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val margin = 5
        val sw = bitmap.width - margin
        val sh = bitmap.height - margin

        outerTop@ for (y in 0 until sh) {
            for (x in 0 until sw) {
                val pixel = pixels[y * bitmap.width + x]
                if(isWhite(pixel)) {
                    top = y
                    break@outerTop
                }
            }
        }

        outerBottom@ for (y in sh - 1 downTo 0) {
            for (x in 0 until sw) {
                val pixel = pixels[y * bitmap.width + x]
                if(isWhite(pixel)) {
                    bottom = y
                    break@outerBottom
                }
            }
        }

        outerLeft@ for (x in margin until sw) {
            for(y in top until bottom) {
                val pixel = pixels[y * bitmap.width + x]
                if(isWhite(pixel)) {
                    left = x
                    break@outerLeft
                }
            }
        }

        outerRight@ for (x in sw-1 downTo margin) {
            for ( y in top until bottom) {
                val pixel = pixels[y * bitmap.width + x]
                if (isWhite(pixel)) {
                    right = x
                    break@outerRight
                }
            }
        }

        val w = right - left
        val h = bottom - top

        val size = minOf(right - left, bottom - top, bitmap.width - left, bitmap.height - top)
        right = left + size
        bottom = top + size

        return Rect(left, top, right, bottom)
    }

    private fun isWhite(pixel: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        return r > 200 && g > 200 && b > 200
    }

    private fun recognizeDigit(bitmap: Bitmap?, row: Int, col: Int) {
        try {
            val processedBM = preprocessImg(bitmap)
            val inImg = InputImage.fromBitmap(processedBM, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(inImg)
                .addOnSuccessListener { txt ->
                    val recText = txt.text.trim()
                    Log.d("OCR", "Raw text at ($row, $col): $recText")

                    val digit = recText.filter { it.isDigit() }
                        .take(1)
                        .toIntOrNull()

                    if (digit != null && digit in 1..9) {
                        recognizedGrid[row][col] = digit
                        Log.d("OCR", "Recognized $digit at position ($row, $col)")
                    } else {
                        Log.d("OCR", "No valid digit found at ($row,$col)")
                    }

                    processedCells++
                    checkCompletion()
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "Text recognition failed at ($row, $col): ${e.message}")
                    processedCells++
                    checkCompletion()
                }
        } catch (e: Exception) {
            Log.e("OCR", "Error at ($row, $col): ${e.message}")
            processedCells++
            checkCompletion()
        }
    }

    private fun checkCompletion() {
        if(processedCells == 81) {
            val gridStr = recognizedGrid.joinToString("\n") {
                row -> row.joinToString(" ")
            }
            onSuccess(gridStr)
        }
    }

    private fun saveBitmapForDebug(bitmap: Bitmap, name: String) {
        try {
            val file = File(
                ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "OCR_Debug"
            )
            if(!file.exists()) file.mkdirs()

            val imgFile = File(file, "$name.png")
            val stream = FileOutputStream(imgFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            Log.d("OCR", "Saved debug image: ${imgFile.absolutePath}")
        } catch(e: Exception) {
            Log.e("OCR", "Failed to save debug image: ${e.message}")
        }
    }
}