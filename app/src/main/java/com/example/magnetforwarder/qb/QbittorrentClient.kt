package com.example.magnetforwarder.qb

import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy

class QbittorrentClient {
    private val cookieJar: CookieJar = JavaNetCookieJar(
        CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) },
    )

    private val http = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()

    @Throws(IOException::class, IllegalArgumentException::class)
    fun login(baseUrl: String, username: String, password: String): LoginResult {
        val loginUrl = normalizeBaseUrl(baseUrl)
            .newBuilder()
            .addPathSegments("api/v2/auth/login")
            .build()

        val body = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()

        val req = Request.Builder()
            .url(loginUrl)
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                return LoginResult.Failure("HTTP ${resp.code}: $text")
            }
            return LoginResult.Success
        }
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun addMagnets(baseUrl: String, magnets: List<String>): AddResult {
        val addUrl = normalizeBaseUrl(baseUrl)
            .newBuilder()
            .addPathSegments("api/v2/torrents/add")
            .build()

        val urls = magnets.joinToString("\n")
        val body = FormBody.Builder()
            .add("urls", urls)
            .add("sequentialDownload", "true")
            .add("firstLastPiecePrio", "true")
            .build()

        val req = Request.Builder()
            .url(addUrl)
            .post(body)
            .build()

        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            return if (resp.isSuccessful) {
                AddResult.Success
            } else {
                AddResult.Failure("HTTP ${resp.code}: $text")
            }
        }
    }

    private fun normalizeBaseUrl(baseUrl: String) =
        (baseUrl.trim().trimEnd('/')).toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid base URL: $baseUrl")
}

sealed class LoginResult {
    data object Success : LoginResult()
    data class Failure(val reason: String) : LoginResult()
}

sealed class AddResult {
    data object Success : AddResult()
    data class Failure(val reason: String) : AddResult()
}

