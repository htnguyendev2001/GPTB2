package com.htanh.appvk.data.remote.sheets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class GoogleSheetsResponseReaderTest {
    @Test
    fun readsUtf8ResponseWhenWithinLimit() {
        val result = ByteArrayInputStream("học tập".toByteArray(Charsets.UTF_8))
            .readUtf8Bounded(64)

        assertEquals("học tập", result)
    }

    @Test
    fun acceptsResponseExactlyAtByteLimit() {
        val result = ByteArrayInputStream("abcd".toByteArray(Charsets.UTF_8))
            .readUtf8Bounded(4)

        assertEquals(4, result.toByteArray(Charsets.UTF_8).size)
    }

    @Test
    fun rejectsResponseLargerThanByteLimit() {
        val error = assertThrows(LearningContentSyncException::class.java) {
            ByteArrayInputStream("abcde".toByteArray(Charsets.UTF_8)).readUtf8Bounded(4)
        }

        assertEquals(LearningContentSyncException.Category.INVALID_CONTENT, error.category)
    }
}
