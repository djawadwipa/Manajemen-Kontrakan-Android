package id.djawadwipa.manajemenkontrakan.util

import id.djawadwipa.manajemenkontrakan.data.local.AppSettingEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun encryptDecrypt_roundTrip() {
        val payload = BackupPayload(createdAtEpochMillis = System.currentTimeMillis(), settings = AppSettingEntity(), units = emptyList(), invoices = emptyList(), payments = emptyList(), categories = emptyList(), expenses = emptyList())
        val encrypted = BackupCrypto.encrypt(payload, "password-kuat".toCharArray())
        val restored = BackupCrypto.decrypt(encrypted, "password-kuat".toCharArray())
        assertEquals(payload.settings, restored.settings)
        assertEquals(1, restored.formatVersion)
    }

    @Test
    fun decrypt_rejectsWrongPassword() {
        val payload = BackupPayload(createdAtEpochMillis = 1L, settings = AppSettingEntity(), units = emptyList(), invoices = emptyList(), payments = emptyList(), categories = emptyList(), expenses = emptyList())
        val encrypted = BackupCrypto.encrypt(payload, "password-benar".toCharArray())
        assertThrows(Exception::class.java) { BackupCrypto.decrypt(encrypted, "password-salah".toCharArray()) }
    }
}
