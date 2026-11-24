package com.hisab.app.ui

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ফোনের Documents/আমার হিসাব ফোল্ডারে ব্যাকআপ রাখা ও সেখান থেকে তালিকা দেখানোর কাজ */
object BackupStore {
    /** Documents-এর ভেতরের ফোল্ডারের নাম */
    const val FOLDER = "আমার হিসাব"
    private const val MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    /** তালিকার একটি ব্যাকআপ ফাইল */
    data class Item(val name: String, val uri: Uri, val modified: Long)

    private fun dir(): File = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), FOLDER)

    /** অ্যান্ড্রয়েড ৯ ও পুরনোতে ফোল্ডার ব্যবহারের জন্য স্টোরেজ অনুমতি লাগে; নতুনে লাগে না */
    fun needsPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED

    /** অ্যাপ চালুর সময় ফোল্ডারটি বানানোর চেষ্টা (ফোন না দিলে প্রথম ব্যাকআপের সময় বানাবে) */
    fun ensureFolder(ctx: Context) {
        try {
            if (needsPermission(ctx)) return
            dir().mkdirs()
        } catch (_: Exception) {
        }
    }

    /** নতুন ব্যাকআপ ফাইলের লেখার স্ট্রিম খোলে (Documents/আমার হিসাব/hisab-backup-তারিখ.xlsx)। না পারলে null */
    fun openForWrite(ctx: Context): OutputStream? {
        val name = "hisab-backup-" + SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date()) + ".xlsx"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, name)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, MIME)
                    put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/" + FOLDER)
                }
                val uri = ctx.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
                uri?.let { ctx.contentResolver.openOutputStream(it) }
            } else {
                val d = dir()
                d.mkdirs()
                FileOutputStream(File(d, name))
            }
        } catch (_: Exception) {
            null
        }
    }

    /** ফোল্ডারের ব্যাকআপ ফাইলগুলো, নতুন আগে */
    fun list(ctx: Context): List<Item> {
        val out = mutableListOf<Item>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val base = MediaStore.Files.getContentUri("external")
                ctx.contentResolver.query(
                    base,
                    arrayOf(
                        MediaStore.Files.FileColumns._ID,
                        MediaStore.Files.FileColumns.DISPLAY_NAME,
                        MediaStore.Files.FileColumns.DATE_MODIFIED,
                    ),
                    "${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?",
                    arrayOf(Environment.DIRECTORY_DOCUMENTS + "/" + FOLDER + "%", "%.xlsx"),
                    "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
                )?.use { c ->
                    while (c.moveToNext()) {
                        out += Item(c.getString(1), ContentUris.withAppendedId(base, c.getLong(0)), c.getLong(2) * 1000)
                    }
                }
            }
        } catch (_: Exception) {
        }
        // সরাসরি ফোল্ডার পড়া গেলে (পুরনো ফোন বা অনুমতি থাকলে) যেগুলো তালিকায় নেই সেগুলোও যোগ
        try {
            if (!needsPermission(ctx)) {
                dir().listFiles { f -> f.isFile && f.name.endsWith(".xlsx", ignoreCase = true) }?.forEach { f ->
                    if (out.none { it.name == f.name }) out += Item(f.name, Uri.fromFile(f), f.lastModified())
                }
            }
        } catch (_: Exception) {
        }
        return out.sortedByDescending { it.modified }
    }
}
