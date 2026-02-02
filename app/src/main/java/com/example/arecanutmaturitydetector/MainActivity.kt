package com.example.arecanutmaturitydetector

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var btnSelect: Button
    private lateinit var btnCamera: Button
    private lateinit var btnPredict: Button
    private lateinit var txtResult: TextView
    private lateinit var txtConfidence: TextView

    private lateinit var classifier: ImageClassifier
    private var selectedBitmap: Bitmap? = null
    private var currentPhotoPath: String? = null

    companion object {
        private const val IMAGE_PICK_CODE = 1001
        private const val CAMERA_REQUEST_CODE = 1002
        private const val CAMERA_PERMISSION_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        btnSelect = findViewById(R.id.btnSelectImage)
        btnCamera = findViewById(R.id.btnCamera)
        btnPredict = findViewById(R.id.btnPredict)
        txtResult = findViewById(R.id.txtResult)
        txtConfidence = findViewById(R.id.txtConfidence)

        classifier = ImageClassifier(this)

        btnSelect.setOnClickListener {
            selectImageFromGallery()
        }

        btnCamera.setOnClickListener {
            checkCameraPermissionAndOpen()
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

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.arecanutmaturitydetector.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    val imageUri: Uri? = data?.data
                    imageUri?.let {
                        selectedBitmap = loadBitmapFromUri(it)
                        imageView.setImageBitmap(selectedBitmap)
                        btnPredict.isEnabled = true
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val file = File(currentPhotoPath)
                    if (file.exists()) {
                        selectedBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                        imageView.setImageBitmap(selectedBitmap)
                        btnPredict.isEnabled = true
                    }
                }
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
