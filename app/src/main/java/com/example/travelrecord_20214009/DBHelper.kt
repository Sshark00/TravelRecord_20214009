package com.example.travelrecord_20214009

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "travel_record.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_NAME = "travel_records"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_DATE = "date"
        const val COL_MEMO = "memo"
        const val COL_PHOTO_PATH = "photo_path"
        const val COL_IS_FAVORITE = "is_favorite"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_DATE TEXT NOT NULL,
                $COL_MEMO TEXT DEFAULT '',
                $COL_PHOTO_PATH TEXT DEFAULT '',
                $COL_IS_FAVORITE INTEGER DEFAULT 0,
                $COL_LATITUDE REAL DEFAULT 0.0,
                $COL_LONGITUDE REAL DEFAULT 0.0
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // CREATE
    fun insertTravel(record: TravelRecord): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, record.title)
            put(COL_DATE, record.date)
            put(COL_MEMO, record.memo)
            put(COL_PHOTO_PATH, record.photoPath)
            put(COL_IS_FAVORITE, if (record.isFavorite) 1 else 0)
            put(COL_LATITUDE, record.latitude)
            put(COL_LONGITUDE, record.longitude)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // READ - 전체 목록
    fun getAllTravels(orderBy: String = "$COL_DATE DESC"): List<TravelRecord> {
        val list = mutableListOf<TravelRecord>()
        val db = readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, orderBy)
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToRecord(it))
            }
        }
        return list
    }

    // READ - 즐겨찾기 목록
    fun getFavoriteTravels(orderBy: String = "$COL_DATE DESC"): List<TravelRecord> {
        val list = mutableListOf<TravelRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, null,
            "$COL_IS_FAVORITE = 1", null, null, null,
            orderBy
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToRecord(it))
            }
        }
        return list
    }

    // READ - GPS 정보가 있는 목록 (지도 마커용)
    fun getTravelsWithLocation(): List<TravelRecord> {
        val list = mutableListOf<TravelRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, null,
            "$COL_LATITUDE != 0.0 OR $COL_LONGITUDE != 0.0", null, null, null,
            "$COL_DATE DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToRecord(it))
            }
        }
        return list
    }

    // READ - 단건 조회
    fun getTravelById(id: Long): TravelRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME, null,
            "$COL_ID = ?", arrayOf(id.toString()), null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToRecord(it) else null
        }
    }

    // UPDATE
    fun updateTravel(record: TravelRecord): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, record.title)
            put(COL_DATE, record.date)
            put(COL_MEMO, record.memo)
            put(COL_PHOTO_PATH, record.photoPath)
            put(COL_IS_FAVORITE, if (record.isFavorite) 1 else 0)
            put(COL_LATITUDE, record.latitude)
            put(COL_LONGITUDE, record.longitude)
        }
        return db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(record.id.toString()))
    }

    // UPDATE - 즐겨찾기 토글
    fun toggleFavorite(id: Long): Int {
        val record = getTravelById(id) ?: return 0
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_FAVORITE, if (record.isFavorite) 0 else 1)
        }
        return db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // DELETE - 단건 삭제
    fun deleteTravel(id: Long): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // DELETE - 전체 삭제
    fun deleteAllTravels(): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, null, null)
    }

    private fun cursorToRecord(cursor: Cursor): TravelRecord {
        return TravelRecord(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)) ?: "",
            date = cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)) ?: "",
            memo = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEMO)) ?: "",
            photoPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHOTO_PATH)) ?: "",
            isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_FAVORITE)) == 1,
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE))
        )
    }
}
