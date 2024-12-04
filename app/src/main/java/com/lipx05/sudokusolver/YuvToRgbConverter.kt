package com.lipx05.sudokusolver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream

class YuvToRgbConverter {

    fun toBitmap(image: Image): Bitmap {
        when (image.planes.size) {
            3 -> {
                val yPlane = image.planes[0].buffer // Y plane
                val uPlane = image.planes[1].buffer // U plane
                val vPlane = image.planes[2].buffer // V plane

                val ySize = yPlane.remaining()
                val uSize = uPlane.remaining()
                val vSize = vPlane.remaining()

                Log.d("YuvToRgbConverter", "Y plane size: $ySize")
                Log.d("YuvToRgbConverter", "U plane size: $uSize")
                Log.d("YuvToRgbConverter", "V plane size: $vSize")

                val nv21 = ByteArray(ySize + uSize + vSize)

                yPlane.get(nv21, 0, ySize)
                val uvSize = uSize.coerceAtMost(vSize)
                val uvData = ByteArray(uvSize*2)
                for(i in 0 until uvSize) {
                    uvData[i * 2] = vPlane.get(i)
                    uvData[(i * 2) + 1] = uPlane.get(i)
                }
                System.arraycopy(uvData, 0, nv21, ySize, uvData.size)

                // NV21 byte tömb -> Bitmap
                val yuvImage = YuvImage(
                    nv21, ImageFormat.NV21, image.width, image.height, null
                )
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(
                    0, 0, image.width, image.height), 100, out
                )
                val imageBytes = out.toByteArray()

                return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            }
            1 -> {
                if (image.format == 0x100) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                else {
                    val yPlane = image.planes[0].buffer
                    val ySize = yPlane.remaining()
                    val yData = ByteArray(ySize)
                    yPlane.get(yData)

                    val bitmap = Bitmap.createBitmap(
                        image.width,
                        image.height,
                        Bitmap.Config.ARGB_8888
                    )
                    val pixels = IntArray(image.width * image.height)

                    for(y in 0 until image.height) {
                        for(x in 0 until image.width) {
                            val pixVal = yData[y*image.width+x].toInt() and 0xFF
                            val color = (pixVal shl 16) or (pixVal shl 8) or pixVal
                            pixels[y*image.width+x] = color
                        }
                    }

                    bitmap.setPixels(
                        pixels, 0, image.width, 0, 0, image.width, image.height
                    )
                    return bitmap
                }
            }
            else -> {
                Log.e("ImageFormat", "Unexpected number of planes: ${image.planes.size}")
                return Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888)
            }
        }
    }
}