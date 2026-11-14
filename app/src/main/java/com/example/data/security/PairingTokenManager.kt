package com.example.data.security

import android.os.Build
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WahjPairingPayload(
    val scheme: String = "WAHJ_PAIR_V1",
    val childTempId: String,
    val childName: String,
    val deviceType: String,
    val deviceModel: String,
    val osVersion: String,
    val token: String,
    val nonce: String,
    val issuedAtMs: Long,
    val expiresAtMs: Long,
    val hostIp: String,
    val port: Int,
    val checksum: String
)

object PairingTokenManager {
    private const val TTL_MS = 5 * 60 * 1000L // 5 minutes temporary QR validity
    private val json = Json { ignoreUnknownKeys = true }

    fun createPayload(
        childTempId: String,
        childName: String,
        deviceType: String,
        hostIp: String,
        port: Int
    ): WahjPairingPayload {
        val now = System.currentTimeMillis()
        val token = CryptoHelper.generateSecureToken(18)
        val nonce = CryptoHelper.generateSecureToken(12)
        val model = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val os = "Android ${Build.VERSION.RELEASE}"
        val exp = expiresAt(now)
        val checksum = CryptoHelper.hashSecret("$childTempId|$nonce|$exp", token)
        return WahjPairingPayload(
            childTempId = childTempId,
            childName = childName.trim(),
            deviceType = deviceType,
            deviceModel = model,
            osVersion = os,
            token = token,
            nonce = nonce,
            issuedAtMs = now,
            expiresAtMs = exp,
            hostIp = hostIp,
            port = port,
            checksum = checksum
        )
    }

    private fun expiresAt(now: Long): Long = now + TTL_MS

    fun encode(payload: WahjPairingPayload): String {
        return json.encodeToString(payload)
    }

    fun decodeAndValidate(raw: String): Result<WahjPairingPayload> {
        return runCatching {
            val trimmed = raw.trim()
            val payload = json.decodeFromString<WahjPairingPayload>(trimmed)
            require(payload.scheme == "WAHJ_PAIR_V1") { "رمز QR غير تابع لتطبيق وهج" }
            require(payload.token.isNotBlank() && payload.nonce.isNotBlank()) { "رمز الأمان غير مكتمل" }
            val expected = CryptoHelper.hashSecret(
                "${payload.childTempId}|${payload.nonce}|${payload.expiresAtMs}",
                payload.token
            )
            require(expected == payload.checksum) { "فشل التحقق من سلامة الرمز المشفر" }
            require(System.currentTimeMillis() <= payload.expiresAtMs) {
                "انتهت صلاحية رمز QR المؤقت. اطلب من الطفل إنشاء رمز جديد."
            }
            payload
        }
    }
}
