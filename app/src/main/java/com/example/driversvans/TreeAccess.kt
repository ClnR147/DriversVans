package com.example.driversvans.storage

import android.content.Context
import android.content.Intent
import android.net.Uri

private const val PREFS = "driver_vans_prefs"
private const val KEY_TREE = "driver_vans_tree"

object TreeAccess {
    fun saveTreeUri(ctx: Context, uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        ctx.contentResolver.takePersistableUriPermission(uri, flags)

        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TREE, uri.toString())
            .apply()
    }

    fun getTreeUri(ctx: Context): Uri? {
        val s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TREE, null)
        return s?.let(Uri::parse)
    }
}
