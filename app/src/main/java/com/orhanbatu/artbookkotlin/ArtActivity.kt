package com.orhanbatu.artbookkotlin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.Snackbar
import com.orhanbatu.artbookkotlin.databinding.ActivityArtBinding
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private var selectedBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info == "old") {
            binding.saveButton.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",0)
            try {

                val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,
                    null)
            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))
                val artNameIx = cursor.getColumnIndex("artname")
                val idIx = cursor.getColumnIndex("id")
                val artistNameIx = cursor.getColumnIndex("artistname")
                val yearIx = cursor.getColumnIndex("year")
                val imageIx = cursor.getColumnIndex("image")

                while (cursor.moveToNext()) {
                    binding.artText.setText(cursor.getString(artNameIx))
                    binding.artistText.setText(cursor.getString(artistNameIx))
                    binding.yearText.setText(cursor.getString(yearIx))

                    val byteArray = cursor.getBlob(imageIx)
                    val bitmapold = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                    val scaledOldBitmap = makeSmallerBitmap(bitmapold, 800)
                    binding.imageView.setImageBitmap(scaledOldBitmap)
                }
                cursor.close()
            }

            catch (e : Exception) {e.printStackTrace()}


        }
        else
        {
            binding.artText.setText("")
            binding.artistText.setText("")
            binding.yearText.setText("")
            binding.imageView.setImageResource(R.drawable.selectimage)
            binding.saveButton.visibility = View.VISIBLE
        }

    }

    fun selectImage (view : View) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.
                READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(view, "This app needs permission", LENGTH_INDEFINITE)
                        .setAction("Give Permission", {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }).show()
                }
                else
                {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            else
            {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.
                EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        }

        else
        //Version is smaller than TIRAMISU
        {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.
            READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(view, "This app needs permission", LENGTH_INDEFINITE)
                    .setAction("Give Permission"
                ) {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }.show()
            }
            else
            {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        else
        {
            val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.
            EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }
    }
    fun saveButtonClicked (view : View) {

        val artName = binding.artText.text.toString()
        val artistName = binding.artistText.text.toString()
        val year = binding.yearText.text.toString()

        if (selectedBitmap != null) {
            val scaledBitmap = makeSmallerBitmap(selectedBitmap!!, 300)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 50,outputStream)
            val byteArray = outputStream.toByteArray()

            //Database
            try {
                val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,
                    null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")
                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            }
            catch (e : Exception) {
                e.printStackTrace()
            }
            val intent = Intent(this@ArtActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

    }
    private fun registerLauncher() {

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(),
            { result ->
            if (result) {
                val intentToGallery = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
            else {
                Toast.makeText(this, "We need permission", Toast.LENGTH_LONG).show()
            }
        })

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            { result ->

                //<I, O>

            if (result.resultCode == RESULT_OK) {
                val intentFromGallery = result.data
                if (intentFromGallery != null) {
                    val imageUri = intentFromGallery.data
                    try {
                        //Converting URI to Bitmap so we can save it to our database
                        if (imageUri != null) {
                            if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(
                                    contentResolver,imageUri)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                            else
                            {
                                selectedBitmap = MediaStore.Images.Media.getBitmap(
                                    contentResolver, imageUri)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })

        }

    private fun makeSmallerBitmap(image : Bitmap, maximumSize : Int) : Bitmap {

        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if (bitmapRatio > 1) {
            //Landscape picture
             width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()


        }
        else  {
            //Portrait picture
            height = maximumSize
            val scaledWidth = width * bitmapRatio
            width = scaledWidth.toInt()

        }

        return Bitmap.createScaledBitmap(image, width, height, false)
    }
    }