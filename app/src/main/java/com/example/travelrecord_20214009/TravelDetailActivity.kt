package com.example.travelrecord_20214009

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import java.io.File

class TravelDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var ivPhoto: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvMemo: TextView

    private var recordId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dbHelper = DBHelper(this)
        recordId = intent.getLongExtra(EXTRA_RECORD_ID, 0)

        if (recordId <= 0) {
            Toast.makeText(this, R.string.error_record_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        ivPhoto = findViewById(R.id.iv_photo)
        tvTitle = findViewById(R.id.tv_title)
        tvDate = findViewById(R.id.tv_date)
        tvMemo = findViewById(R.id.tv_memo)
        val btnEdit = findViewById<MaterialButton>(R.id.btn_edit)

        toolbar.setNavigationOnClickListener { finish() }
        btnEdit.setOnClickListener {
            val intent = Intent(this, TravelAddEditActivity::class.java).apply {
                putExtra(TravelAddEditActivity.EXTRA_RECORD_ID, recordId)
            }
            startActivity(intent)
        }

        loadRecord()
    }

    override fun onResume() {
        super.onResume()
        if (recordId > 0 && ::dbHelper.isInitialized) {
            loadRecord()
        }
    }

    private fun loadRecord() {
        val record = dbHelper.getTravelById(recordId) ?: run {
            Toast.makeText(this, R.string.error_record_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTitle.text = record.title
        tvDate.text = record.date
        tvMemo.text = record.memo.ifBlank { getString(R.string.memo_empty) }
        bindPhoto(record.photoPath)
    }

    private fun bindPhoto(photoPath: String) {
        if (photoPath.isNotBlank() && File(photoPath).exists()) {
            ivPhoto.setImageBitmap(BitmapFactory.decodeFile(photoPath))
        } else {
            ivPhoto.setImageResource(R.drawable.ic_photo_placeholder)
        }
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }
}
