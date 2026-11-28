package com.htanh.appvk.data.remote.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidApiKeyIdentityTest {
    @Test
    fun sha1Hex_returnsUppercaseHexWithoutSeparators() {
        val result = AndroidApiKeyIdentity.sha1Hex("abc".toByteArray())

        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", result)
    }

    @Test
    fun sha1Hex_returnsFortyHexCharacters() {
        val result = AndroidApiKeyIdentity.sha1Hex(byteArrayOf(0))

        assertEquals(40, result.length)
        assertTrue(result.matches(Regex("[0-9A-F]{40}")))
    }
}
