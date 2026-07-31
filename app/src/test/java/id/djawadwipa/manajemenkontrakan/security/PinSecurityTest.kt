package id.djawadwipa.manajemenkontrakan.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {
    @Test
    fun createdPinCanBeVerified() {
        val record = PinSecurity.create("246810")

        assertTrue(
            PinSecurity.verify(
                pin = "246810",
                encodedSalt = record.salt,
                encodedHash = record.hash,
            ),
        )
        assertFalse(
            PinSecurity.verify(
                pin = "135790",
                encodedSalt = record.salt,
                encodedHash = record.hash,
            ),
        )
    }

    @Test
    fun identicalPinsUseDifferentSalts() {
        val first = PinSecurity.create("1234")
        val second = PinSecurity.create("1234")

        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
    }

    @Test
    fun pinFormatRequiresFourToEightDigits() {
        assertTrue(PinSecurity.isValidFormat("1234"))
        assertTrue(PinSecurity.isValidFormat("12345678"))
        assertFalse(PinSecurity.isValidFormat("123"))
        assertFalse(PinSecurity.isValidFormat("123456789"))
        assertFalse(PinSecurity.isValidFormat("12ab"))
    }
}
