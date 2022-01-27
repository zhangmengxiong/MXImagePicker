package com.mx.imgpicker.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mx.imgpicker.models.DbSourceItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.source_loader.MXImageSource
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File

class MXSourceDB(val context: Context) {
    private val dbHelp by lazy { DBHelp(context.applicationContext).writableDatabase }
    fun addSource(file: File, type: MXPickerType): Boolean {
        try {
            val values = ContentValues()
            values.put(DBHelp.DB_KEY_PATH, file.absolutePath)
            values.put(DBHelp.DB_KEY_TYPE, type.name)
            values.put(DBHelp.DB_KEY_TIME, System.currentTimeMillis())
            values.put(DBHelp.DB_KEY_VIDEO_LENGTH, 0)
            return dbHelp.insert(DBHelp.DB_NAME, null, values) >= 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun getAllSource(type: MXPickerType): ArrayList<DbSourceItem> {
        var cursor: Cursor? = null
        val sourceList = ArrayList<DbSourceItem>()
        var section: String? = null
        var sectionArg: Array<String>? = null

        if (type != MXPickerType.ImageAndVideo) {
            section = "${DBHelp.DB_KEY_TYPE}=?"
            sectionArg = arrayOf(type.name)
        } else {
            section = ""
            sectionArg = emptyArray()
        }

        try {
            cursor = dbHelp.query(
                DBHelp.DB_NAME, arrayOf(
                    DBHelp.DB_KEY_PATH,
                    DBHelp.DB_KEY_TIME,
                    DBHelp.DB_KEY_VIDEO_LENGTH,
                    DBHelp.DB_KEY_TYPE
                ), section, sectionArg, null, null, null
            )
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val item = cursorToItem(cursor)
                    if (item != null) {
                        sourceList.add(item)
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                cursor?.close()
            } catch (e: Exception) {
            }
        }
        return sourceList
    }

    private fun cursorToItem(cursor: Cursor): DbSourceItem? {
        try {
            val type =
                MXPickerType.from(cursor.getString(cursor.getColumnIndexOrThrow(DBHelp.DB_KEY_TYPE)))
            val mimeType = if (type == MXPickerType.Video) {
                MXVideoSource.MIME_TYPE
            } else {
                MXImageSource.MIME_TYPE
            }
            val path = cursor.getString(cursor.getColumnIndexOrThrow(DBHelp.DB_KEY_PATH))
            val time = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelp.DB_KEY_TIME))
            var video_length = cursor.getLong(cursor.getColumnIndexOrThrow(DBHelp.DB_KEY_VIDEO_LENGTH))

            val file = File(path)
            if (type == MXPickerType.Video && video_length <= 0 && file.exists()) {
                video_length = MXVideoSource.getVideoLength(file)
                if (video_length > 0) {
                    dbHelp.update(DBHelp.DB_NAME, ContentValues().apply {
                        put(DBHelp.DB_KEY_VIDEO_LENGTH, video_length)
                    }, "${DBHelp.DB_KEY_PATH}=?", arrayOf(path))
                }
            }

            if (path.isNotBlank() && time > 0L) {
                return DbSourceItem(
                    path,
                    type,
                    mimeType,
                    time,
                    (video_length / 1000).toInt()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private class DBHelp(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {
        companion object {
            const val DB_NAME = "picker_db"
            const val DB_KEY_PATH = "picker_path"
            const val DB_KEY_TYPE = "picker_type"
            const val DB_KEY_TIME = "create_time"
            const val DB_KEY_VIDEO_LENGTH = "video_length"
        }

        override fun onCreate(db: SQLiteDatabase?) {
            db?.execSQL("create table $DB_NAME($DB_KEY_PATH varchar(500) , $DB_KEY_TYPE varchar(20) , $DB_KEY_TIME long, $DB_KEY_VIDEO_LENGTH long)")
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

        }
    }
}

