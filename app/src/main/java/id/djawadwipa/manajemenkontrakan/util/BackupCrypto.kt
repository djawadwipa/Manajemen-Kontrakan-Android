package id.djawadwipa.manajemenkontrakan.util

import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseCategoryEntity
import id.djawadwipa.manajemenkontrakan.data.local.ExpenseEntity
import id.djawadwipa.manajemenkontrakan.data.local.InvoiceEntity
import id.djawadwipa.manajemenkontrakan.data.local.PaymentEntity
import id.djawadwipa.manajemenkontrakan.data.local.RentalUnitEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class BackupPayload(
    val formatVersion: Int = 1,
    val createdAtEpochMillis: Long,
    val settings: AppSettingEntity,
    val units: List<RentalUnitEntity>,
    val invoices: List<InvoiceEntity>,
    val payments: List<PaymentEntity>,
    val categories: List<ExpenseCategoryEntity>,
    val expenses: List<ExpenseEntity>,
)

object BackupCrypto {
    private val magic = "MKBACKUP1".encodeToByteArray()
    private const val saltLength = 16
    private const val ivLength = 12
    private const val hashLength = 32
    private const val iterations = 210_000
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun encrypt(payload: BackupPayload, password: CharArray): ByteArray {
        require(password.size >= 8) { "Kata sandi minimal 8 karakter." }
        val salt = ByteArray(saltLength).also(SecureRandom()::nextBytes)
        val iv = ByteArray(ivLength).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(magic)
        val ciphertext = cipher.doFinal(json.encodeToString(payload).encodeToByteArray())
        val digest = MessageDigest.getInstance("SHA-256").digest(ciphertext)
        return ByteBuffer.allocate(magic.size + salt.size + iv.size + digest.size + ciphertext.size)
            .put(magic).put(salt).put(iv).put(digest).put(ciphertext).array()
    }

    fun decrypt(bytes: ByteArray, password: CharArray): BackupPayload {
        require(bytes.size > magic.size + saltLength + ivLength + hashLength) { "File backup tidak valid." }
        val buffer = ByteBuffer.wrap(bytes)
        val fileMagic = ByteArray(magic.size).also(buffer::get)
        require(fileMagic.contentEquals(magic)) { "Format backup tidak dikenali." }
        val salt = ByteArray(saltLength).also(buffer::get)
        val iv = ByteArray(ivLength).also(buffer::get)
        val expectedHash = ByteArray(hashLength).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val actualHash = MessageDigest.getInstance("SHA-256").digest(ciphertext)
        require(MessageDigest.isEqual(expectedHash, actualHash)) { "Checksum SHA-256 tidak cocok." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(magic)
        val plain = cipher.doFinal(ciphertext).decodeToString()
        return json.decodeFromString(BackupPayload.serializer(), plain)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(encoded, "AES")
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
        }
    }
}
