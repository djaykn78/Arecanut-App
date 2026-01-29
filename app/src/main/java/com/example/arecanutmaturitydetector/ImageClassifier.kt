package com.example.arecanutmaturitydetector

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class ImageClassifier(context: Context) {

    companion object {
        private const val MODEL_NAME = "arecanut_model.tflite"
        private const val INPUT_SIZE = 224
        private const val PIXEL_SIZE = 3
        private const val FLOAT_SIZE = 4
    }

    private val interpreter: Interpreter

    init {
        val modelBuffer = loadModelFile(context)
        interpreter = Interpreter(modelBuffer)
    }

    private fun loadModelFile(context: Context): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_NAME)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(inputBuffer: ByteBuffer): Pair<String, Float> {
        val output = Array(1) { FloatArray(1) }
        interpreter.run(inputBuffer, output)

        val raw = output[0][0]   // sigmoid output between 0 and 1

        val label: String
        val confidence: Float

        if (raw >= 0.5f) {
            label = "Unripen"
            confidence = raw
        } else {
            label = "Ripen"
            confidence = (1 - raw)
        }

        return Pair(label, confidence)
    }


    fun close() {
        interpreter.close()
    }
}
