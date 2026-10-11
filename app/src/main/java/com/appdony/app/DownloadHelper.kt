package com.appdony.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import java.io.File

data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val downloaded: Long,
    val total: Long,
    val localUri: String?
)

class DownloadHelper(private val context: Context) {

    val manager: DownloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun startDownload(url: String, userAgent: String?, contentDisposition: String?, mimetype: String?): Long {
        return try {
            if (url.startsWith("blob:") || url.startsWith("data:")) return -1
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)

            CookieManager.getInstance().getCookie(url)?.let {
                request.addRequestHeader("Cookie", it)
            }
            userAgent?.let { request.addRequestHeader("User-Agent", it) }
            request.addRequestHeader("Referer", "https://appdony.ir/")

            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            request.setTitle(fileName)
            request.setDescription("در حال دانلود از اپدونی")
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            mimetype?.let { request.setMimeType(it) }

            manager.enqueue(request)
        } catch (e: Exception) {
            -1
        }
    }

    fun getAll(): List<DownloadItem> {
        val items = mutableListOf<DownloadItem>()
        val cursor = manager.query(DownloadManager.Query())
        while (cursor.moveToNext()) {
            items.add(DownloadItem(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "بدون نام",
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            ))
        }
        cursor.close()
        return items.reversed()
    }

    fun getActiveCount(): Int {
        var count = 0
        val cursor = manager.query(DownloadManager.Query())
        while (cursor.moveToNext()) {
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) count++
        }
        cursor.close()
        return count
    }

    fun remove(id: Long) {
        manager.remove(id)
        getSharedPreferences().edit().remove("icon_$id").apply()
    }

    fun saveIcon(id: Long, url: String) {
        getSharedPreferences().edit().putString("icon_$id", url).apply()
    }

    fun getIcon(id: Long): String? {
        return getSharedPreferences().getString("icon_$id", null)
    }

    private fun getSharedPreferences() =
        context.getSharedPreferences("appdony_icons", Context.MODE_PRIVATE)

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
