package com.appdony.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import org.json.JSONObject

data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val downloaded: Long,
    val total: Long,
    val localUri: String?,
    val isCancelled: Boolean = false,
    val savedUrl: String? = null,
    val savedIconUrl: String? = null
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

            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)
            request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )

            manager.enqueue(request)
        } catch (e: Exception) {
            -1
        }
    }

    // گرفتن URL واقعی از DownloadManager قبل از لغو
    fun getUrl(id: Long): String {
        return try {
            val cursor = manager.query(DownloadManager.Query().setFilterById(id))
            if (cursor.moveToFirst()) {
                val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI))
                cursor.close()
                uri ?: ""
            } else {
                cursor.close()
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    // لغو دانلود — URL واقعی رو از DownloadManager می‌گیریم
    fun cancel(id: Long, title: String, iconUrl: String?) {
        val realUrl = getUrl(id)

        try {
            manager.remove(id)
        } catch (_: Exception) {}

        val json = JSONObject().apply {
            put("title", title)
            put("url", realUrl)
            put("icon", iconUrl ?: "")
        }
        getPrefs().edit().putString("cancelled_$id", json.toString()).apply()
        getPrefs().edit().remove("icon_$id").apply()
    }

    // شروع مجدد
    fun restartCancelled(id: Long): Long {
        val raw = getPrefs().getString("cancelled_$id", null) ?: return -1
        val json = try { JSONObject(raw) } catch (_: Exception) { return -1 }
        val url = json.optString("url", "")
        val iconUrl = json.optString("icon", "")

        if (url.isEmpty()) {
            getPrefs().edit().remove("cancelled_$id").apply()
            return -1
        }

        val newId = startDownload(url, null, null, null)
        if (newId > 0) {
            getPrefs().edit().remove("cancelled_$id").apply()
            if (iconUrl.isNotEmpty()) saveIcon(newId, iconUrl)
        }
        return newId
    }

    fun removePermanently(id: Long) {
        try { manager.remove(id) } catch (_: Exception) {}
        getPrefs().edit().remove("cancelled_$id").apply()
        getPrefs().edit().remove("icon_$id").apply()
    }

    fun getAll(): List<DownloadItem> {
        val items = mutableListOf<DownloadItem>()
        val cursor = manager.query(DownloadManager.Query())
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
            val title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "بدون نام"
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            items.add(DownloadItem(id, title, status, downloaded, total, localUri))
        }
        cursor.close()

        // افزودن لغو‌شده‌ها
        getPrefs().all.forEach { (key, value) ->
            if (key.startsWith("cancelled_") && value is String) {
                val id = key.removePrefix("cancelled_").toLongOrNull() ?: return@forEach
                val json = try { JSONObject(value) } catch (_: Exception) { return@forEach }
                items.add(DownloadItem(
                    id = id,
                    title = json.optString("title", "لغو شده"),
                    status = -100,
                    downloaded = 0,
                    total = 0,
                    localUri = null,
                    isCancelled = true,
                    savedUrl = json.optString("url", ""),
                    savedIconUrl = json.optString("icon", "")
                ))
            }
        }

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

    fun saveIcon(id: Long, url: String) {
        getPrefs().edit().putString("icon_$id", url).apply()
    }

    fun getIcon(id: Long): String? = getPrefs().getString("icon_$id", null)

    private fun getPrefs() = context.getSharedPreferences("appdony_downloads", Context.MODE_PRIVATE)

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
