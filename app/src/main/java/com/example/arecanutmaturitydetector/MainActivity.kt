package com.example.arecanutmaturitydetector

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btnSelect: Button
    private lateinit var btnPredict: Button
    private lateinit var txtResult: TextView
    private lateinit var txtConfidence: TextView

    private lateinit var classifier: ImageClassifier
    private var selectedBitmap: Bitmap? = null

    companion object {
        private const val IMAGE_PICK_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        btnSelect = findViewById(R.id.btnSelectImage)
        btnPredict = findViewById(R.id.btnPredict)
        txtResult = findViewById(R.id.txtResult)
        txtConfidence = findViewById(R.id.txtConfidence)

        classifier = ImageClassifier(this)

        btnSelect.setOnClickListener {
            selectImageFromGallery()
        }

        btnPredict.setOnClickListener {
            selectedBitmap?.let {
                runInference(it)
            }
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            imageUri?.let {
                selectedBitmap = loadBitmapFromUri(it)
                imageView.setImageBitmap(selectedBitmap)
                btnPredict.isEnabled = true
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(this.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        }

        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }


    private fun runInference(bitmap: Bitmap) {
        val inputBuffer = ImageUtils.bitmapToByteBuffer(bitmap)
        val (label, confidence) = classifier.classify(inputBuffer)

        txtResult.text = "Result: $label"
        txtConfidence.text = "Confidence: ${"%.2f".format(confidence * 100)}%"
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}
