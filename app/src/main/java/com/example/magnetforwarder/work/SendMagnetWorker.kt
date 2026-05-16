package com.example.magnetforwarder.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.magnetforwarder.data.SecureSettings
import com.example.magnetforwarder.qb.AddResult
import com.example.magnetforwarder.qb.LoginResult
import com.example.magnetforwarder.qb.QbittorrentClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class SendMagnetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val magnets = inputData.getStringArray(KEY_MAGNET_URLS)?.toList().orEmpty()
            .map { it.trim() }
            .filter { it.startsWith("magnet:?") }
            .distinct()

        if (magnets.isEmpty()) {
            return@withContext Result.failure(
                Data.Builder().putString(KEY_ERROR, "No magnet URLs provided").build(),
            )
        }

        val settings = SecureSettings(applicationContext)
        val baseUrl = settings.baseUrl
        val username = settings.username
        val password = settings.password

        if (password.isBlank() || baseUrl.isBlank() || username.isBlank()) {
            Notifier.notifyFailure(applicationContext, "Configure qBittorrent settings first")
            return@withContext Result.failure(
                Data.Builder().putString(KEY_ERROR, "Missing settings").build(),
            )
        }

        val client = QbittorrentClient()
        try {
            when (val login = client.login(baseUrl, username, password)) {
                is LoginResult.Failure -> {
                    Notifier.notifyFailure(applicationContext, "Login failed: ${login.reason}")
                    return@withContext Result.failure(
                        Data.Builder().putString(KEY_ERROR, login.reason).build(),
                    )
                }
                LoginResult.Success -> Unit
            }

            when (val add = client.addMagnets(baseUrl, magnets)) {
                is AddResult.Failure -> {
                    Notifier.notifyFailure(applicationContext, "Add failed: ${add.reason}")
                    return@withContext Result.failure(
                        Data.Builder().putString(KEY_ERROR, add.reason).build(),
                    )
                }
                AddResult.Success -> {
                    Notifier.notifySuccess(applicationContext, magnets.size)
                    return@withContext Result.success()
                }
            }
        } catch (e: IOException) {
            Notifier.notifyFailure(applicationContext, "Network error: ${e.message ?: e.javaClass.simpleName}")
            return@withContext Result.retry()
        } catch (e: IllegalArgumentException) {
            Notifier.notifyFailure(applicationContext, e.message ?: "Invalid configuration")
            return@withContext Result.failure(
                Data.Builder().putString(KEY_ERROR, e.message ?: "Invalid config").build(),
            )
        }
    }

    companion object {
        const val KEY_MAGNET_URLS = "magnet_urls"
        const val KEY_ERROR = "error"
    }
}

