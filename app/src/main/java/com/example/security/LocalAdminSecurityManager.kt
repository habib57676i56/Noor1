package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class LocalAdminRole(
    val key: String,
    val labelAr: String,
    val labelEn: String
) {
    SUPER_ADMIN("SUPER_ADMIN", "مدير النظام العام", "Super Admin"),
    TEACHER("TEACHER", "معلم حلقات القرآن", "Quran Teacher"),
    EDITOR("EDITOR", "محرر المحتوى والمكتبة", "Content Editor");

    fun canManageAccounts(): Boolean = this == SUPER_ADMIN
    fun canBackupAndRestore(): Boolean = this == SUPER_ADMIN
    fun canApproveContent(): Boolean = this == SUPER_ADMIN || this == EDITOR
    fun canManageAiKnowledge(): Boolean = this == SUPER_ADMIN || this == EDITOR
    fun canManageLibrary(): Boolean = this == SUPER_ADMIN || this == EDITOR
    fun canManageQuizzes(): Boolean = this == SUPER_ADMIN || this == EDITOR || this == TEACHER
    fun canManageHalaqat(): Boolean = this == SUPER_ADMIN || this == TEACHER
    fun canViewAuditLogs(): Boolean = this == SUPER_ADMIN

    companion object {
        fun fromKey(key: String?): LocalAdminRole =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: EDITOR
    }
}

data class PasswordDigest(
    val hashHex: String,
    val saltHex: String
)

/**
 * Cryptographic security utility for the Offline-First Local Super Admin system.
 *
 * Security guarantees:
 * - Zero hardcoded passwords or backdoors.
 * - Salted PBKDF2WithHmacSHA256 (65,536 iterations, 256-bit derived key, 128-bit SecureRandom salt).
 * - Constant-time byte array comparison via MessageDigest.isEqual to prevent timing attacks.
 * - SHA-256 content fingerprinting for duplicate knowledge detection.
 */
object LocalAdminSecurityManager {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 65_536
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val MIN_PASSCODE_LENGTH = 6

    private val secureRandom = SecureRandom()

    fun isStrongEnoughPasscode(passcode: String): Boolean {
        val trimmed = passcode.trim()
        return trimmed.length >= MIN_PASSCODE_LENGTH
    }

    fun hashPasscode(passcode: String): PasswordDigest {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val hash = pbkdf2(passcode.toCharArray(), salt)
        return PasswordDigest(
            hashHex = bytesToHex(hash),
            saltHex = bytesToHex(salt)
        )
    }

    fun verifyPasscode(passcode: String, storedHashHex: String, storedSaltHex: String): Boolean {
        if (passcode.isEmpty() || storedHashHex.isBlank() || storedSaltHex.isBlank()) return false
        val salt = hexToBytes(storedSaltHex) ?: return false
        val expectedHash = hexToBytes(storedHashHex) ?: return false
        val actualHash = pbkdf2(passcode.toCharArray(), salt)
        return MessageDigest.isEqual(actualHash, expectedHash)
    }

    fun sha256Hex(text: String): String {
        val normalized = text.trim().lowercase().replace(Regex("\\s+"), " ")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return bytesToHex(digest)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val chars = CharArray(bytes.size * 2)
        val hexChars = "0123456789abcdef"
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            chars[i * 2] = hexChars[v ushr 4]
            chars[i * 2 + 1] = hexChars[v and 0x0F]
        }
        return String(chars)
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(hex[i * 2], 16)
            val lo = Character.digit(hex[i * 2 + 1], 16)
            if (hi < 0 || lo < 0) return null
            out[i] = ((hi shl 4) + lo).toByte()
        }
        return out
    }
}
