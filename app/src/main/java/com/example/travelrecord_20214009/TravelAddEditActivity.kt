package com.example.travelrecord_20214009

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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

        toolbar.title = if (recordId > 0) {
            getString(R.string.edit_travel_title)
        } else {
            getString(R.string.add_travel_title)
        }
        toolbar.setNavigationOnClickListener { finish() }

        etDate.setOnClickListener { showDatePicker() }
        btnSave.setOnClickListener { saveRecord() }

        if (recordId > 0) {
            loadRecord()
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
        if (photoPath.isNotBlank() && File(photoPath).exists()) {
            ivPhoto.setImageBitmap(BitmapFactory.decodeFile(photoPath))
        } else {
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

        if (recordId > 0) {
            dbHelper.updateTravel(record)
            Toast.makeText(this, R.string.msg_updated, Toast.LENGTH_SHORT).show()
        } else {
            dbHelper.insertTravel(record)
            Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }
}
