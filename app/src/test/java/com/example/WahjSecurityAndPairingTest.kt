package com.example

import com.example.data.security.CryptoHelper
import com.example.data.security.PairingTokenManager
import com.example.system.LocationHelper
import org.junit.Assert.*
import org.junit.Test

class WahjSecurityAndPairingTest {

    @Test
    fun passwordHashAndVerificationWork() {
        val salt = CryptoHelper.generateSalt()
        val hash = CryptoHelper.hashSecret("WahjStrongPass123", salt)
        assertTrue(CryptoHelper.verifySecret("WahjStrongPass123", salt, hash))
        assertFalse(CryptoHelper.verifySecret("WrongPass", salt, hash))
    }

    @Test
    fun pairingQrPayloadRoundTripsAndRejectsTampering() {
        val (payload, raw) = PairingTokenManager.createPayload(
            childTempId = "child-1",
            childName = "عمر",
            deviceType = "هاتف",
            hostIp = "192.168.1.25",
            port = 8765
        ) to ""
        val encoded = PairingTokenManager.encode(payload)
        val decoded = PairingTokenManager.decodeAndValidate(encoded)
        assertTrue(decoded.isSuccess)
        assertEquals("عمر", decoded.getOrThrow().childName)

        // Tamper with childTempId -> checksum must fail
        val tampered = encoded.replace("child-1", "child-2")
        assertTrue(PairingTokenManager.decodeAndValidate(tampered).isFailure)
        assertTrue(raw.isEmpty())
    }

    @Test
    fun geofenceDistanceCalculatesReasonableMeters() {
        // ~111m difference for 0.001 deg latitude
        val dist = LocationHelper.distanceMeters(24.7136, 46.6753, 24.7146, 46.6753)
        assertTrue(dist in 100.0..120.0)
    }
}
