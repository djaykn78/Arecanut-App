package com.example.arecanutmaturitydetector

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ImageUtils {

    private const val INPUT_SIZE = 224
    private const val PIXEL_SIZE = 3
    private const val FLOAT_SIZE = 4

    fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {

        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )

        val byteBuffer = ByteBuffer.allocateDirect(
            FLOAT_SIZE * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(
            intValues,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        var pixelIndex = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {

                val pixel = intValues[pixelIndex++]

                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                byteBuffer.putFloat((r / 127.5f) - 1.0f)
                byteBuffer.putFloat((g / 127.5f) - 1.0f)
                byteBuffer.putFloat((b / 127.5f) - 1.0f)

            }
        }

        // 🔥 THIS LINE PREVENTS CRASH
        byteBuffer.rewind()

        return byteBuffer
    }

}

