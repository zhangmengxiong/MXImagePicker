package com.mx.imgpicker.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class MXSQLiteOpenHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {
    companion object {
        internal const val DB_NAME = "mx_image_picker_db_v2"
        internal const val DB_KEY_PATH = "picker_path"
        internal const val DB_KEY_TYPE = "picker_type"
        internal const val DB_KEY_PRIVATE = "private"
        internal const val DB_KEY_TIME = "create_time"
        internal const val DB_KEY_VIDEO_LENGTH = "video_length"

        internal const val VALUE_PRIVATE_APP = "1"
        internal const val VALUE_PRIVATE_SYS = "0"

        private const val DB_CREATE =
            "create table $DB_NAME(" +
                    "$DB_KEY_PATH varchar(500) PRIMARY KEY," +
                    "$DB_KEY_TYPE varchar(20) ," +
                    "$DB_KEY_PRIVATE varchar(2) ," +
                    "$DB_KEY_TIME long," +
                    "$DB_KEY_VIDEO_LENGTH long" +
                    ")"

        private const val DB_DROP = "DROP TABLE IF EXISTS $DB_NAME"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(DB_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(DB_DROP)
        db?.execSQL(DB_CREATE)
    }
}