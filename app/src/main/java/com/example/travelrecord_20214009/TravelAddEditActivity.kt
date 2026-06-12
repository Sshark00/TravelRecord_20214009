package com.example.travelrecord_20214009

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TravelAddEditActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var etTitle: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var etMemo: TextInputEditText
    private lateinit var ivPhoto: ImageView

    private var recordId: Long = 0
    private var photoPath: String = ""
    private var isFavorite: Boolean = false
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var cameraPhotoFile: File? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraPhotoFile?.let { file ->
                photoPath = file.absolutePath
                extractGpsFromPath(photoPath)
                bindPhoto()
            }
        } else {
            Toast.makeText(this, R.string.error_camera_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            handleGalleryImage(uri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasReadPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (hasReadPermission) {
            launchGallery()
        } else {
            Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DBHelper(this)
        recordId = intent.getLongExtra(EXTRA_RECORD_ID, 0)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        etTitle = findViewById(R.id.et_title)
        etDate = findViewById(R.id.et_date)
        etMemo = findViewById(R.id.et_memo)
        ivPhoto = findViewById(R.id.iv_photo)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save)
        val btnCamera = findViewById<MaterialButton>(R.id.btn_camera)
        val btnGallery = findViewById<MaterialButton>(R.id.btn_gallery)

        toolbar.title = if (recordId > 0) {
            getString(R.string.edit_travel_title)
        } else {
            getString(R.string.add_travel_title)
        }
        toolbar.setNavigationOnClickListener { finish() }

        etDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { saveRecord() }
        btnCamera.setOnClickListener { requestCamera() }
        btnGallery.setOnClickListener { requestGallery() }

        if (recordId > 0) {
            loadRecord()
        }
    }

    private fun requestCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestGallery() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }

        val needRequest = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needRequest) {
            galleryPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            launchGallery()
        }
    }

    private fun launchCamera() {
        try {
            val file = createImageFile()
            cameraPhotoFile = file
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_camera_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun handleGalleryImage(uri: Uri) {
        extractGpsFromUri(uri)

        val savedPath = copyImageToInternalStorage(uri)
        if (savedPath != null) {
            photoPath = savedPath
            if (latitude == 0.0 && longitude == 0.0) {
                extractGpsFromPath(photoPath)
            }
            bindPhoto()
        } else {
            Toast.makeText(this, R.string.error_gallery_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val dir = File(filesDir, "images").apply { mkdirs() }
        val fileName = "travel_${System.currentTimeMillis()}.jpg"
        return File(dir, fileName)
    }

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val file = createImageFile()
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun extractGpsFromPath(path: String) {
        try {
            val exif = ExifInterface(path)
            val latLong = FloatArray(2)
            if (exif.getLatLong(latLong)) {
                latitude = latLong[0].toDouble()
                longitude = latLong[1].toDouble()
            }
        } catch (_: Exception) {
            // GPS 정보 없음
        }
    }

    private fun extractGpsFromUri(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    latitude = latLong[0].toDouble()
                    longitude = latLong[1].toDouble()
                }
            }
        } catch (_: Exception) {
            // GPS 정보 없음
        }
    }

    private fun loadRecord() {
        val record = dbHelper.getTravelById(recordId) ?: run {
            Toast.makeText(this, R.string.error_record_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etTitle.setText(record.title)
        etDate.setText(record.date)
        etMemo.setText(record.memo)
        photoPath = record.photoPath
        isFavorite = record.isFavorite
        latitude = record.latitude
        longitude = record.longitude
        bindPhoto()
    }

    private fun bindPhoto() {
        try {
            if (photoPath.isNotBlank() && File(photoPath).exists()) {
                val bitmap = BitmapFactory.decodeFile(photoPath)
                if (bitmap != null) {
                    ivPhoto.setImageBitmap(bitmap)
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_photo_placeholder)
                }
            } else {
                ivPhoto.setImageResource(R.drawable.ic_photo_placeholder)
            }
        } catch (_: Exception) {
            ivPhoto.setImageResource(R.drawable.ic_photo_placeholder)
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        etDate.text?.toString()?.takeIf { it.isNotBlank() }?.let { dateText ->
            try {
                dateFormat.parse(dateText)?.let { calendar.time = it }
            } catch (_: Exception) {
                // keep current calendar
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                etDate.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun saveRecord() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        val date = etDate.text?.toString()?.trim().orEmpty()
        val memo = etMemo.text?.toString()?.trim().orEmpty()

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_title_required, Toast.LENGTH_SHORT).show()
            return
        }
        if (date.isEmpty()) {
            Toast.makeText(this, R.string.error_date_required, Toast.LENGTH_SHORT).show()
            return
        }

        val record = TravelRecord(
            id = recordId,
            title = title,
            date = date,
            memo = memo,
            photoPath = photoPath,
            isFavorite = isFavorite,
            latitude = latitude,
            longitude = longitude
        )

        try {
            if (recordId > 0) {
                val updated = dbHelper.updateTravel(record)
                if (updated <= 0) {
                    throw IllegalStateException("update failed")
                }
                Toast.makeText(this, R.string.msg_updated, Toast.LENGTH_SHORT).show()
            } else {
                val insertedId = dbHelper.insertTravel(record)
                if (insertedId <= 0) {
                    throw IllegalStateException("insert failed")
                }
                Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
            }
            finish()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }
}
