package com.mx.imgpicker.utils.source_loader

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

object MXContentProvide {
    fun createCursor(
        contentResolver: ContentResolver,
        collection: Uri,
        projection: Array<String>,
        whereCondition: String,
        whereArgs: Array<String>,
        orderBy: String,
        orderAscending: Boolean,
    ): Cursor? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
            val selection = createSelectionBundle(
                whereCondition,
                whereArgs,
                orderBy,
                orderAscending
            )
            contentResolver.query(collection, projection, selection, null)
        }
        else -> {
            val orderDirection = if (orderAscending) "ASC" else "DESC"
            val order = "$orderBy $orderDirection"
            contentResolver.query(collection, projection, whereCondition, whereArgs, order)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createSelectionBundle(
        whereCondition: String,
        whereArgs: Array<String>,
        orderBy: String,
        orderAscending: Boolean,
    ): Bundle = Bundle().apply {
        // Sort function
        putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(orderBy))

        // Sorting direction
        val orderDirection = if (orderAscending) {
            ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
        } else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
        putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, orderDirection)
        // Selection
        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, whereCondition)
        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, whereArgs)
    }
}