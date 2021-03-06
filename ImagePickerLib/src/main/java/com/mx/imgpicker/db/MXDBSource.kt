package com.mx.imgpicker.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import java.io.File

internal class MXDBSource(val context: Context) {
    companion object {
        private val lock = Object()
    }

    private val dbHelp by lazy { MXSQLiteOpenHelper(context.applicationContext) }

    /**
     * 添加临时缓存文件
     */
    fun addPrivateSource(file: File, type: MXPickerType): Boolean {
        synchronized(lock) {
            val database = dbHelp.writableDatabase
            try {
                val values = ContentValues()
                values.put(MXSQLiteOpenHelper.DB_KEY_PATH, file.absolutePath)
                values.put(MXSQLiteOpenHelper.DB_KEY_DIR, file.parentFile?.absolutePath)
                values.put(MXSQLiteOpenHelper.DB_KEY_TYPE, type.value)
                values.put(MXSQLiteOpenHelper.DB_KEY_PRIVATE, MXSQLiteOpenHelper.VALUE_PRIVATE_APP)
                values.put(MXSQLiteOpenHelper.DB_KEY_TIME, (System.currentTimeMillis() / 1000))
                values.put(MXSQLiteOpenHelper.DB_KEY_VIDEO_LENGTH, 0)
                return database.replace(MXSQLiteOpenHelper.DB_NAME, null, values) >= 0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * 批量添加/替换系统视频、图片数据
     */
    fun addSysSource(list: List<MXItem>): Boolean {
        synchronized(lock) {
            val database = dbHelp.writableDatabase
            val insertSql =
                "replace into ${MXSQLiteOpenHelper.DB_NAME}(" +
                        "${MXSQLiteOpenHelper.DB_KEY_PATH}, " +
                        "${MXSQLiteOpenHelper.DB_KEY_DIR}, " +
                        "${MXSQLiteOpenHelper.DB_KEY_TYPE}, " +
                        "${MXSQLiteOpenHelper.DB_KEY_PRIVATE}, " +
                        "${MXSQLiteOpenHelper.DB_KEY_TIME}, " +
                        "${MXSQLiteOpenHelper.DB_KEY_VIDEO_LENGTH}) " +
                        "values(?,?,?,?,?,?)"
            val stat = database.compileStatement(insertSql)
            database.beginTransaction()
            try {
                for (item in list) {
                    stat.bindString(1, item.path)
                    stat.bindString(2, File(item.path).parentFile?.absolutePath)
                    stat.bindString(3, item.type.value)
                    stat.bindString(4, MXSQLiteOpenHelper.VALUE_PRIVATE_SYS)
                    stat.bindLong(5, item.time)
                    stat.bindLong(6, item.duration.toLong())
                    stat.executeInsert()
                }
                database.setTransactionSuccessful()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                database.endTransaction()
                database.close()
            }
        }
        return false
    }

    fun getLimitTime(type: MXPickerType): Pair<Long, Long>? {
        synchronized(lock) {
            val database = dbHelp.writableDatabase
            var cursor: Cursor? = null
            try {
                val selectSql =
                    "select max(${MXSQLiteOpenHelper.DB_KEY_TIME}) as 'max_time'," +
                            "min(${MXSQLiteOpenHelper.DB_KEY_TIME}) as 'min_time' " +
                            "from ${MXSQLiteOpenHelper.DB_NAME} where " +
                            "${MXSQLiteOpenHelper.DB_KEY_TYPE} = ? and ${MXSQLiteOpenHelper.DB_KEY_PRIVATE} = ? "
                cursor = database.rawQuery(
                    selectSql, arrayOf(
                        type.value, MXSQLiteOpenHelper.VALUE_PRIVATE_SYS
                    )
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val max_time = cursor.getLong(cursor.getColumnIndexOrThrow("max_time"))
                    val min_time = cursor.getLong(cursor.getColumnIndexOrThrow("min_time"))
                    if (max_time > 0 && min_time > 0 && max_time != min_time) {
                        return Pair(min_time, max_time)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    cursor?.close()
                } catch (e: Exception) {
                }
                database.close()
            }
        }
        return null
    }

    /**
     * 获取对应类型的所有数据
     */
    fun getAllSource(type: MXPickerType): ArrayList<MXItem> {
        val sourceList = ArrayList<MXItem>()
        var section: String? = null
        var sectionArg: Array<String>? = null

        if (type != MXPickerType.ImageAndVideo) {
            section = "${MXSQLiteOpenHelper.DB_KEY_TYPE}=?"
            sectionArg = arrayOf(type.value)
        } else {
            section = ""
            sectionArg = emptyArray()
        }
        val orderBy = MXSQLiteOpenHelper.DB_KEY_TIME + " desc"

        synchronized(lock) {
            val database = dbHelp.writableDatabase
            var cursor: Cursor? = null
            try {
                cursor = database.query(
                    MXSQLiteOpenHelper.DB_NAME, arrayOf(
                        MXSQLiteOpenHelper.DB_KEY_PATH,
                        MXSQLiteOpenHelper.DB_KEY_TIME,
                        MXSQLiteOpenHelper.DB_KEY_PRIVATE,
                        MXSQLiteOpenHelper.DB_KEY_VIDEO_LENGTH,
                        MXSQLiteOpenHelper.DB_KEY_TYPE
                    ), section, sectionArg, null, null, orderBy
                )
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val item = cursorToItem(database, cursor)
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
                database.close()
            }
        }
        return sourceList
    }

    fun getAllDirs(): ArrayList<File> {
        val dirs = ArrayList<File>()
        synchronized(lock) {
            val database = dbHelp.writableDatabase
            var cursor: Cursor? = null
            try {
                val selectSql =
                    "select ${MXSQLiteOpenHelper.DB_KEY_DIR} from ${MXSQLiteOpenHelper.DB_NAME} group by ${MXSQLiteOpenHelper.DB_KEY_DIR}"
                cursor = database.rawQuery(selectSql, arrayOf())
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        val dir = cursor.getString(
                            cursor.getColumnIndexOrThrow(MXSQLiteOpenHelper.DB_KEY_DIR)
                        )
                        val dirFile = File(dir)
                        if (!dirFile.exists()) continue
                        val child = dirFile.listFiles()
                        if (child == null || child.isEmpty()) continue
                        dirs.add(dirFile)
                    } while (cursor.moveToNext())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    cursor?.close()
                } catch (e: Exception) {
                }
                database.close()
            }
        }
        return dirs
    }

    /**
     * 指针转换成对象
     */
    private fun cursorToItem(database: SQLiteDatabase, cursor: Cursor): MXItem? {
        try {
            val type = MXPickerType.from(
                cursor.getString(cursor.getColumnIndexOrThrow(MXSQLiteOpenHelper.DB_KEY_TYPE))
            )

            val isPrivate =
                cursor.getString(cursor.getColumnIndexOrThrow(MXSQLiteOpenHelper.DB_KEY_PRIVATE)) == MXSQLiteOpenHelper.VALUE_PRIVATE_APP
            val path =
                cursor.getString(cursor.getColumnIndexOrThrow(MXSQLiteOpenHelper.DB_KEY_PATH))
            var duration =
                cursor.getLong(cursor.getColumnIndexOrThrow(MXSQLiteOpenHelper.DB_KEY_VIDEO_LENGTH))

            val file = File(path)
            if (!file.exists() && !isPrivate) {
                database.delete(
                    MXSQLiteOpenHelper.DB_NAME,
                    "${MXSQLiteOpenHelper.DB_KEY_PATH} = ?",
                    arrayOf(path)
                )
                return null
            }
            val time = file.lastModified()

            if (type == MXPickerType.Video && duration <= 0) {
                duration = MXVideoSource.getVideoLength(file) / 1000
                if (duration > 0) {
                    database.update(MXSQLiteOpenHelper.DB_NAME, ContentValues().apply {
                        put(MXSQLiteOpenHelper.DB_KEY_VIDEO_LENGTH, duration)
                    }, "${MXSQLiteOpenHelper.DB_KEY_PATH}=?", arrayOf(path))
                }
            }

            if (path.isNotBlank() && time > 0L) {
                return MXItem(path, time, type, duration.toInt())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

