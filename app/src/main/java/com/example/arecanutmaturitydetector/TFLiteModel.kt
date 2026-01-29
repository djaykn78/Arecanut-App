package com.example.arecanutmaturitydetector

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModel(context: Context) {

    private val tflite: Interpreter

    init {
        val modelFile = loadModelFile(context)
        tflite = Interpreter(modelFile)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("model.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(input: Array<Array<Array<FloatArray>>>): Float {
        val output = Array(1) { FloatArray(1) }
        tflite.run(input, output)
        return output[0][0]
    }
}
