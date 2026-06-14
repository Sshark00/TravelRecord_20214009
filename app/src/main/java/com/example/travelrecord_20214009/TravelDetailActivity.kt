package com.example.travelrecord_20214009

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnFavorite: MaterialButton

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
        btnFavorite = findViewById(R.id.btn_favorite)
        val btnEdit = findViewById<MaterialButton>(R.id.btn_edit)

        toolbar.setNavigationOnClickListener { finish() }
        btnEdit.setOnClickListener {
            val intent = Intent(this, TravelAddEditActivity::class.java).apply {
                putExtra(TravelAddEditActivity.EXTRA_RECORD_ID, recordId)
            }
            startActivity(intent)
        }
        btnFavorite.setOnClickListener { toggleFavorite() }
        findViewById<MaterialButton>(R.id.btn_delete).setOnClickListener { showDeleteDialog() }

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
        updateFavoriteButton(record.isFavorite)
    }

    private fun toggleFavorite() {
        try {
            val updated = dbHelper.toggleFavorite(recordId)
            if (updated <= 0) {
                Toast.makeText(this, R.string.error_record_not_found, Toast.LENGTH_SHORT).show()
                return
            }
            val record = dbHelper.getTravelById(recordId) ?: return
            updateFavoriteButton(record.isFavorite)
            val message = if (record.isFavorite) {
                R.string.msg_favorite_added
            } else {
                R.string.msg_favorite_removed
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        if (isFavorite) {
            btnFavorite.text = getString(R.string.btn_favorite_remove)
            btnFavorite.setIconResource(R.drawable.ic_favorite)
        } else {
            btnFavorite.text = getString(R.string.btn_favorite_add)
            btnFavorite.setIconResource(R.drawable.ic_favorite_border)
        }
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, tvTitle.text))
            .setPositiveButton(R.string.btn_delete) { _, _ -> deleteRecord() }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun deleteRecord() {
        try {
            val deleted = dbHelper.deleteTravel(recordId)
            if (deleted > 0) {
                finish()
            } else {
                Toast.makeText(this, R.string.error_record_not_found, Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindPhoto(photoPath: String) {
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

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }
}
