package com.appriyo.amarsavings.data.backup

import com.appriyo.amarsavings.data.auth.GoogleAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around the Drive REST v3 API for managing a single backup file
 * inside the user's `appDataFolder`. We avoid the heavy Google Drive SDK to
 * keep the APK small and the dependency tree clean.
 *
 * The `appDataFolder` is *hidden* — the file is not visible from
 * drive.google.com and can only be read by this app's OAuth client.
 */
class DriveBackupClient(
    private val http: OkHttpClient,
    private val auth: GoogleAuthClient,
    private val json: Json
) {

    /**
     * Uploads [bytes] as [BACKUP_FILENAME] to the user's appDataFolder.
     * If a file with the same name already exists, it is updated in place so
     * we maintain a single canonical backup per account.
     *
     * Returns the Drive `fileId` for the uploaded file.
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
            val url = if (existingId != null) {
                BASE + "/files/" + existingId + "?uploadType=multipart&fields=id"
            } else {
                BASE + "/files?uploadType=multipart&fields=id"
            }
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "multipart/related; boundary=$BOUNDARY")
                .put(multipart)
                .build()
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