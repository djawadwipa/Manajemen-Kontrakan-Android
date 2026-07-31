package id.djawadwipa.manajemenkontrakan.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurity {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    data class PinRecord(
        val salt: String,
        val hash: String,
    )

    fun isValidFormat(pin: String): Boolean =
        pin.length in 4..8 && pin.all(Char::isDigit)

    fun create(pin: String): PinRecord {
        require(isValidFormat(pin)) {
            "PIN harus terdiri dari 4–8 angka."
        }
        val salt = ByteArray(SALT_LENGTH_BYTES).also {
            SecureRandom().nextBytes(it)
        }
        val hash = derive(pin, salt)
        val encoder = Base64.getEncoder().withoutPadding()
        return PinRecord(
            salt = encoder.encodeToString(salt),
            hash = encoder.encodeToString(hash),
        )
    }

    fun verify(
        pin: String,
        encodedSalt: String,
        encodedHash: String,
    ): Boolean {
        if (!isValidFormat(pin) || encodedSalt.isBlank() || encodedHash.isBlank()) {
            return false
        }
        return runCatching {
            val decoder = Base64.getDecoder()
            val expected = decoder.decode(encodedHash)
            val actual = derive(pin, decoder.decode(encodedSalt))
            MessageDigest.isEqual(expected, actual)
        }.getOrDefault(false)
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH_BITS,
        )
        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}
