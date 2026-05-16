package com.example.magnetforwarder

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.magnetforwarder.work.SendMagnetWorker

class MagnetHandlerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val magnets = extractMagnets(intent)
        if (magnets.isEmpty()) {
            Toast.makeText(this, "No magnet link found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val req = OneTimeWorkRequestBuilder<SendMagnetWorker>()
            .setInputData(workDataOf(SendMagnetWorker.KEY_MAGNET_URLS to magnets.toTypedArray()))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(req)
        Toast.makeText(this, "Forwarding to qBittorrent…", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun extractMagnets(intent: Intent?): List<String> {
        if (intent == null) return emptyList()
        val magnets = linkedSetOf<String>()

        intent.dataString
            ?.takeIf { it.startsWith("magnet:?") }
            ?.let { magnets += it }

        intent.getStringExtra(Intent.EXTRA_TEXT)
            ?.let { text ->
                magnets += text
                    .split(Regex("\\s+"))
                    .mapNotNull { token ->
                        when {
                            token.startsWith("magnet:?") -> token.trim().trimEnd('.', ',', ')', ']', '>')
                            else -> null
                        }
                    }
            }

        intent.getStringArrayListExtra(Intent.EXTRA_STREAM)?.let { list ->
            // Rare, but some share flows use arrays; keep magnet-like entries only.
            magnets += list.filter { it.startsWith("magnet:?") }
        }

        return magnets.toList()
    }
}

