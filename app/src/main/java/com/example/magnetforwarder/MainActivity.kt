package com.example.magnetforwarder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.magnetforwarder.data.SecureSettings
import com.example.magnetforwarder.qb.LoginResult
import com.example.magnetforwarder.qb.QbittorrentClient
import com.example.magnetforwarder.ui.theme.MagnetForwarderTheme
import com.example.magnetforwarder.work.SendMagnetWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MagnetForwarderTheme {
                SettingsScreen(
                    settings = SecureSettings(this),
                    enqueueMagnet = { magnet ->
                        val req = OneTimeWorkRequestBuilder<SendMagnetWorker>()
                            .setInputData(workDataOf(SendMagnetWorker.KEY_MAGNET_URLS to arrayOf(magnet)))
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .build()
                        WorkManager.getInstance(applicationContext).enqueue(req)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    settings: SecureSettings,
    enqueueMagnet: (String) -> Unit,
) {
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var username by remember { mutableStateOf(settings.username) }
    var password by remember { mutableStateOf(settings.password) }
    var magnet by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // Ensure we render whatever is stored, even if defaults changed between app versions.
        baseUrl = settings.baseUrl
        username = settings.username
        password = settings.password
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("MagnetForwarder") }) },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
        ) {
            Text("qBittorrent WebUI", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL (e.g. http://tower:8080)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    settings.baseUrl = baseUrl
                    settings.username = username
                    settings.password = password
                    scope.launch { snackbars.showSnackbar("Saved") }
                },
            ) {
                Text("Save")
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            try {
                                QbittorrentClient().login(baseUrl, username, password)
                            } catch (t: Throwable) {
                                LoginResult.Failure(t.message ?: t.javaClass.simpleName)
                            }
                        }
                        when (result) {
                            is LoginResult.Failure -> snackbars.showSnackbar("Login failed: ${result.reason}")
                            LoginResult.Success -> snackbars.showSnackbar("Login OK")
                        }
                    }
                },
            ) {
                Text("Test connection")
            }

            Spacer(Modifier.height(24.dp))
            Text("Quick send", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = magnet,
                onValueChange = { magnet = it },
                label = { Text("Magnet URL (magnet:?)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = magnet.trim().startsWith("magnet:?"),
                onClick = {
                    val m = magnet.trim()
                    enqueueMagnet(m)
                    scope.launch { snackbars.showSnackbar("Forwarding…") }
                },
            ) {
                Text("Send magnet")
            }
        }
    }
}

