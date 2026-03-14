package com.himanshoe.tracey

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.himanshoe.tracey.platform.AndroidContext

/**
 * A zero-surface [ContentProvider] that captures the [android.content.Context]
 * before any application code runs.
 *
 * Android guarantees that all [ContentProvider]s are created before
 * [android.app.Application.onCreate] is called. By registering this provider in
 * the library's `AndroidManifest.xml` with a high `initOrder`, Tracey can
 * initialise its context reference without requiring the host app to pass a
 * `Context` anywhere.
 *
 * This mirrors the pattern used by Firebase, LeakCanary, and WorkManager.
 * The provider is marked `exported=false` and all query/insert/update/delete
 * methods throw [UnsupportedOperationException] so it cannot be used as an
 * actual content provider.
 */
internal class TraceyInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        AndroidContext.app = ctx.applicationContext
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
