package com.mx.imgpicker.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.mx.imgpicker.models.MXDirItem
import com.mx.imgpicker.models.MXItem
import com.mx.imgpicker.models.MXPickerType
import com.mx.imgpicker.utils.source_loader.MXVideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

internal class MXDBSource(val context: Context) {
    companion object {
        private val lock = Object()
    }

    private val dbHelp by lazy { MXSQLite(context.applicationContext) }

    /**
     * 添加临时缓存文件
     */
    suspend fun addPrivateSource(file: File, type: MXPickerType): Boolean =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val database = dbHelp.writableDatabase
                try {
                    val values = ContentValues()
                    values.put(MXSQLite.DB_PATH, file.absolutePath)
                    values.put(MXSQLite.DB_DIR, file.parentFile?.absolutePath)
                    values.put(MXSQLite.DB_TYPE, type.value)
                    values.put(
                        MXSQLite.DB_PRIVATE,
                        MXSQLite.VALUE_PRIVATE_APP
                    )
                    values.put(MXSQLite.DB_TIME, System.currentTimeMillis())
                    values.put(MXSQLite.DB_VIDEO_LENGTH, 0)
                    return@withContext database.replace(
                        MXSQLite.DB_NAME,
                        null,
                        values
                    ) >= 0
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return@withContext false
        }

    /**
     * 批量添加/替换系统视频、图片数据
     */
    suspend fun addSysSource(list: List<MXItem>): Boolean =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val database = dbHelp.writableDatabase
                val insertSql =
                    "replace into ${MXSQLite.DB_NAME}(" +
                            "${MXSQLite.DB_PATH}, " +
                            "${MXSQLite.DB_DIR}, " +
                            "${MXSQLite.DB_TYPE}, " +
                            "${MXSQLite.DB_PRIVATE}, " +
                            "${MXSQLite.DB_TIME}, " +
                            "${MXSQLite.DB_VIDEO_LENGTH}) " +
                            "values(?,?,?,?,?,?)"
                val stat = database.compileStatement(insertSql)
                database.beginTransaction()
                try {
                    for (item in list) {
                        stat.bindString(1, item.path)
                        stat.bindString(2, File(item.path).parentFile?.absolutePath)
                        stat.bindString(3, item.type.value)
                        stat.bindString(4, MXSQLite.VALUE_PRIVATE_SYS)
                        stat.bindLong(5, item.timeInMs)
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
            return@withContext false
        }

    /**
     * 获取对应类型的所有数据
     */
    suspend fun getAllSource(
        type: MXPickerType,
        path: String,
        maxListSize: Int
    ): ArrayList<MXItem> =
        withContext(Dispatchers.IO) {
            val sourceList = ArrayList<MXItem>()
            val section = ArrayList<String>()
            val sectionArg = ArrayList<String>()

            if (type != MXPickerType.ImageAndVideo) {
                section.add("${MXSQLite.DB_TYPE}=?")
                sectionArg.add(type.value)
            }
            if (path.isNotBlank()) {
                section.add("${MXSQLite.DB_DIR}=?")
                sectionArg.add(path)
            }
            val orderBy =
                MXSQLite.DB_TIME + " desc" + (if (maxListSize > 0) " limit $maxListSize" else "")

            synchronized(lock) {
                val database = dbHelp.writableDatabase
                var cursor: Cursor? = null
                try {
                    cursor = database.query(
                        MXSQLite.DB_NAME,
                        arrayOf(
                            MXSQLite.DB_PATH,
                            MXSQLite.DB_TIME,
                            MXSQLite.DB_PRIVATE,
                            MXSQLite.DB_VIDEO_LENGTH,
                            MXSQLite.DB_TYPE
                        ),
                        section.joinToString(" and "),
                        sectionArg.toTypedArray(),
                        null,
                        null,
                        orderBy
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
            return@withContext sourceList
        }

    suspend fun getAllDirList(type: MXPickerType): ArrayList<MXDirItem> =
        withContext(Dispatchers.IO) {
            val dirs = ArrayList<MXDirItem>()
            synchronized(lock) {
                val database = dbHelp.writableDatabase
                var cursor: Cursor? = null
                try {
                    val selectSql =
                        if (type != MXPickerType.ImageAndVideo) "select ${MXSQLite.DB_DIR}, count(${MXSQLite.DB_PATH}) as count " +
                                "from  ${MXSQLite.DB_NAME} " +
                                "where ${MXSQLite.DB_TYPE}=? " +
                                "group by ${MXSQLite.DB_DIR} " +
                                "order by count desc;"
                        else "select ${MXSQLite.DB_DIR}, count(${MXSQLite.DB_PATH}) as count " +
                                "from  ${MXSQLite.DB_NAME} " +
                                "group by ${MXSQLite.DB_DIR} " +
                                "order by count desc;"
                    cursor = database.rawQuery(
                        selectSql,
                        if (type != MXPickerType.ImageAndVideo) arrayOf(type.name) else null
                    )
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            val dir = cursor.getString(
                                cursor.getColumnIndexOrThrow(MXSQLite.DB_DIR)
                            )
                            val count = cursor.getInt(
                                cursor.getColumnIndexOrThrow("count")
                            )
                            if (count <= 0) continue

                            val dirFile = File(dir)
                            if (!dirFile.exists()) continue

                            dirs.add(MXDirItem(dirFile.nameWithoutExtension, dir, count, null))
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
            return@withContext dirs
        }

    suspend fun queryLastItem(path: String, type: MXPickerType): MXItem? =
        withContext(Dispatchers.IO) {
            val section = ArrayList<String>()
            val sectionArg = ArrayList<String>()

            if (type != MXPickerType.ImageAndVideo) {
                section.add("${MXSQLite.DB_TYPE}=?")
                sectionArg.add(type.value)
            }
            if (path.isNotBlank()) {
                section.add("${MXSQLite.DB_DIR}=?")
                sectionArg.add(path)
            }
            val orderBy = MXSQLite.DB_TIME + " desc"

            synchronized(lock) {
                val database = dbHelp.writableDatabase
                var cursor: Cursor? = null
                try {
                    cursor = database.query(
                        MXSQLite.DB_NAME,
                        arrayOf(
                            MXSQLite.DB_PATH,
                            MXSQLite.DB_TIME,
                            MXSQLite.DB_PRIVATE,
                            MXSQLite.DB_VIDEO_LENGTH,
                            MXSQLite.DB_TYPE
                        ),
                        section.joinToString(" and "),
                        sectionArg.toTypedArray(),
                        null,
                        null,
                        orderBy
                    )
                    if (cursor != null && cursor.moveToFirst()) {
                        do {
                            val item = cursorToItem(database, cursor)
                            if (item != null) {
                                return@withContext item
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
            return@withContext null
        }

    /**
     * 指针转换成对象
     */
    private fun cursorToItem(database: SQLiteDatabase, cursor: Cursor): MXItem? {
        try {
            val type = MXPickerType.from(
                cursor.getString(cursor.getColumnIndexOrThrow(MXSQLite.DB_TYPE))
            )

            val isPrivate =
                cursor.getString(cursor.getColumnIndexOrThrow(MXSQLite.DB_PRIVATE)) == MXSQLite.VALUE_PRIVATE_APP
            val path =
                cursor.getString(cursor.getColumnIndexOrThrow(MXSQLite.DB_PATH))
            val time =
                cursor.getLong(cursor.getColumnIndexOrThrow(MXSQLite.DB_TIME))
            var duration =
                cursor.getLong(cursor.getColumnIndexOrThrow(MXSQLite.DB_VIDEO_LENGTH))

            val file = File(path)
            if (!file.exists() || file.length() <= 0) {
                if (!isPrivate) {
                    database.delete(
                        MXSQLite.DB_NAME,
                        "${MXSQLite.DB_PATH} = ?",
                        arrayOf(path)
                    )
                }
                if (isPrivate && abs(System.currentTimeMillis() - time) > 60 * 1000) {// 删除超时的自拍数据
                    database.delete(
                        MXSQLite.DB_NAME,
                        "${MXSQLite.DB_PATH} = ?",
                        arrayOf(path)
                    )
                }
                return null
            }

            if (type == MXPickerType.Video && duration <= 0) {
                duration = MXVideoSource.getVideoLength(file) / 1000
                if (duration > 0) {
                    database.update(MXSQLite.DB_NAME, ContentValues().apply {
                        put(MXSQLite.DB_VIDEO_LENGTH, duration)
                    }, "${MXSQLite.DB_PATH}=?", arrayOf(path))
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

