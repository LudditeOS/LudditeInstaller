package com.example.ludditeinstaller

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

class ApkDownloadManager(private val context: Context) {
    private var downloadId: Long = 0
    private lateinit var onDownloadComplete: (Boolean) -> Unit

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                context?.unregisterReceiver(this)
                onDownloadComplete(true)
            }
        }
    }

    fun downloadAndInstall(url: String, fileName: String, onComplete: (Boolean) -> Unit) {
        onDownloadComplete = onComplete

        // Register download receiver
        context.registerReceiver(
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        // Create download request
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Downloading APK")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        // Start download
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
    }

    fun getDownloadedApkFile(fileName: String): File {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadDir, fileName)
    }
}

// Extension function to install APK
fun Context.installApk(apkFile: File) {
    val apkUri = FileProvider.getUriForFile(
        this,
        "${packageName}.provider",
        apkFile
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    startActivity(intent)
}