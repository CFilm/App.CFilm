package com.example.peliselprimazo.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var downloadId: Long = -1

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun downloadAndInstall(url: String, fileName: String = "CFilm_Update.apk") {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) {
            file.delete()
        }

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Actualización CFilm")
                .setDescription("Descargando nueva versión...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadId = downloadManager.enqueue(request)
            _isDownloading.value = true
            _downloadProgress.value = 0f
            _error.value = null

            trackProgress(file)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        checkDownloadStatus(file)
                        try {
                            context.unregisterReceiver(this)
                        } catch (_: Exception) {}
                    }
                }
            }

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            _error.value = "Error al iniciar la descarga: ${e.message}"
            _isDownloading.value = false
        }
    }

    private fun checkDownloadStatus(file: File) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                _downloadProgress.value = 1f
                _isDownloading.value = false
                cursor.close()
                installApk(file)
                return
            } else if (status == DownloadManager.STATUS_FAILED) {
                val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                _error.value = "Descarga fallida (Código: $reason)"
                _isDownloading.value = false
                cursor.close()
                return
            }
        }
        cursor?.close()
        if (file.exists() && file.length() > 0) {
            _downloadProgress.value = 1f
            _isDownloading.value = false
            installApk(file)
        }
    }

    private fun trackProgress(file: File) {
        CoroutineScope(Dispatchers.IO).launch {
            while (_isDownloading.value) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = if (statusIndex != -1) cursor.getInt(statusIndex) else -1

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        _downloadProgress.value = 1f
                        _isDownloading.value = false
                        cursor.close()
                        installApk(file)
                        break
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                        _error.value = "Descarga fallida (Código: $reason)"
                        _isDownloading.value = false
                        cursor.close()
                        break
                    }

                    val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    
                    if (downloadedIndex != -1 && totalIndex != -1) {
                        val bytesDownloaded = cursor.getInt(downloadedIndex)
                        val bytesTotal = cursor.getInt(totalIndex)
                        
                        if (bytesTotal > 0) {
                            val progress = (bytesDownloaded.toFloat() / bytesTotal.toFloat()).coerceIn(0f, 1f)
                            _downloadProgress.value = progress
                            if (bytesDownloaded >= bytesTotal) {
                                _downloadProgress.value = 1f
                                _isDownloading.value = false
                                cursor.close()
                                installApk(file)
                                break
                            }
                        }
                    }
                }
                cursor?.close()
                delay(300)
            }
        }
    }

    fun installApk(file: File) {
        if (!file.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
