package com.appriyo.amarsavings.data.backup

import com.appriyo.amarsavings.data.auth.DriveAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class DriveBackupClient(
    private val http: OkHttpClient,
    private val auth: DriveAuthClient,
    private val json: Json
) {

    /**
     * Uploads [bytes] as [BACKUP_FILENAME] to the user's appDataFolder.
     * If a file with the same name already exists, its content is updated
     * (PATCH) in place; otherwise a new one is created (POST).
     *
     * IMPORTANT: content uploads (uploadType=multipart) must go to the
     * separate `/upload/drive/v3` host — the plain `/drive/v3` host only
     * serves metadata/read operations and returns a generic HTML 404 if you
     * send an upload-type request to it.
     */
    suspend fun upload(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val existingId = findBackupFileId()
            val metadata = buildString {
                append("{\"name\":\"").append(BACKUP_FILENAME).append('"')
                if (existingId == null) {
                    append(",\"parents\":[\"appDataFolder\"]")
                }
                append('}')
            }
            val token = auth.getAccessToken()
                ?: error("No access token — sign in first")
            val multipart = buildMultipart(metadata, bytes)

            val requestBuilder = Request.Builder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "multipart/related; boundary=$BOUNDARY")

            val req = if (existingId != null) {
                // Update existing file's content.
                requestBuilder
                    .url("$UPLOAD_BASE/files/$existingId?uploadType=multipart&fields=id")
                    .patch(multipart)
                    .build()
            } else {
                // Create a new file.
                requestBuilder
                    .url("$UPLOAD_BASE/files?uploadType=multipart&fields=id")
                    .post(multipart)
                    .build()
            }

            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) error("Drive upload failed (${resp.code}): $body")
                val id = REGEX_ID.find(body)?.groupValues?.getOrNull(1)
                    ?: error("Drive upload: no file id in response: $body")
                id
            }
        }
    }

    /**
     * Downloads and parses the backup file, or returns null if no backup exists.
     * Content reads use the plain (non-upload) host.
     */
    suspend fun download(): Result<BackupFile?> = withContext(Dispatchers.IO) {
        runCatching {
            val id = findBackupFileId() ?: return@runCatching null
            val token = auth.getAccessToken()
                ?: error("No access token — sign in first")
            val req = Request.Builder()
                .url("${BASE}/files/${id}?alt=media")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@use null
                val body = resp.body?.string()
                if (!resp.isSuccessful) error("Drive download failed (${resp.code}): $body")
                if (body.isNullOrBlank()) null
                else json.decodeFromString(BackupFile.serializer(), body)
            }
        }
    }

    /**
     * Returns metadata about the existing backup file, or null if none.
     */
    suspend fun getMeta(): Result<BackupMeta?> = withContext(Dispatchers.IO) {
        runCatching {
            val token = auth.getAccessToken()
                ?: error("No access token — sign in first")
            val q = "name='$BACKUP_FILENAME' and 'appDataFolder' in parents and trashed=false"
            val req = Request.Builder()
                .url("$BASE/files?spaces=appDataFolder&q=${java.net.URLEncoder.encode(q, "UTF-8")}&fields=files(id,modifiedTime)")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Drive list failed (${resp.code})")
                val body = resp.body?.string().orEmpty()
                val id = REGEX_ID.find(body)?.groupValues?.getOrNull(1) ?: return@use null
                BackupMeta(id, modifiedTimeMs = null)
            }
        }
    }

    private suspend fun findBackupFileId(): String? = getMeta().getOrNull()?.fileId

    private fun buildMultipart(metadata: String, content: ByteArray) =
        buildString {
            append("--").append(BOUNDARY).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata).append("\r\n")
            append("--").append(BOUNDARY).append("\r\n")
            append("Content-Type: application/json\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)
            .let { prefix ->
                prefix + content + "\r\n--$BOUNDARY--\r\n".toByteArray(Charsets.UTF_8)
            }
            .toRequestBody("multipart/related; boundary=$BOUNDARY".toMediaType())

    companion object {
        private const val BASE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
        private const val BACKUP_FILENAME = "amar_savings_backup.json"
        private const val BOUNDARY = "amar_savings_backup_boundary"
        private val REGEX_ID = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        fun defaultJson(): Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
}