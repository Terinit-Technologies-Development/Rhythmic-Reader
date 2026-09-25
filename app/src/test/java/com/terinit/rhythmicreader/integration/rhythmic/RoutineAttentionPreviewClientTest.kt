package com.terinit.rhythmicreader.integration.rhythmic

import android.database.MatrixCursor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutineAttentionPreviewClientTest {

    private val dateKey = "2026-09-26"

    @Test
    fun `parses Routine's versioned next-target row`() {
        val cursor = cursorFor(
            mapOf(
                RoutineAttentionPreviewProtocol.COLUMN_PROTOCOL_VERSION to RoutineAttentionPreviewProtocol.PROTOCOL_VERSION,
                RoutineAttentionPreviewProtocol.COLUMN_DATE_KEY to dateKey,
                RoutineAttentionPreviewProtocol.COLUMN_NEXT_COOLDOWN_ORDINAL to 4,
                RoutineAttentionPreviewProtocol.COLUMN_REQUIRED_ACTIVE_SECONDS to 5_400L,
                RoutineAttentionPreviewProtocol.COLUMN_REQUIRED_QUALIFIED_PAGES to 47,
            )
        )

        val preview = cursor.use {
            assertTrue(it.moveToFirst())
            RoutineAttentionPreviewProtocol.parsePreview(it, dateKey)
        }

        assertEquals(dateKey, preview?.dateKey)
        assertEquals(4, preview?.nextCooldownOrdinal)
        assertEquals(5_400L, preview?.requiredActiveSeconds)
        assertEquals(47, preview?.requiredQualifiedPages)
    }

    @Test
    fun `rejects malformed dates and stale or unsupported provider responses`() {
        val staleCursor = cursorFor(
            mapOf(
                RoutineAttentionPreviewProtocol.COLUMN_PROTOCOL_VERSION to RoutineAttentionPreviewProtocol.PROTOCOL_VERSION,
                RoutineAttentionPreviewProtocol.COLUMN_DATE_KEY to "2026-09-25",
                RoutineAttentionPreviewProtocol.COLUMN_NEXT_COOLDOWN_ORDINAL to 4,
                RoutineAttentionPreviewProtocol.COLUMN_REQUIRED_ACTIVE_SECONDS to 5_400L,
                RoutineAttentionPreviewProtocol.COLUMN_REQUIRED_QUALIFIED_PAGES to 47,
            )
        )
        val unsupportedCursor = cursorFor(
            mapOf(
                RoutineAttentionPreviewProtocol.COLUMN_PROTOCOL_VERSION to 99,
                RoutineAttentionPreviewProtocol.COLUMN_DATE_KEY to dateKey,
                RoutineAttentionPreviewProtocol.COLUMN_NEXT_COOLDOWN_ORDINAL to 4,
                RoutineAttentionPreviewProtocol.COLUMN_REQUIRED_ACTIVE_SECONDS to 5_400L,
                RoutineAttentionPreviewProtocol.COLUMN_REQUIRED_QUALIFIED_PAGES to 47,
            )
        )

        assertFalse(RoutineAttentionPreviewProtocol.isValidDateKey("2026-02-30"))
        staleCursor.use {
            assertTrue(it.moveToFirst())
            assertNull(RoutineAttentionPreviewProtocol.parsePreview(it, dateKey))
        }
        unsupportedCursor.use {
            assertTrue(it.moveToFirst())
            assertNull(RoutineAttentionPreviewProtocol.parsePreview(it, dateKey))
        }
    }

    private fun cursorFor(values: Map<String, Any>): MatrixCursor =
        MatrixCursor(RoutineAttentionPreviewProtocol.ALL_COLUMNS).apply {
            addRow(RoutineAttentionPreviewProtocol.ALL_COLUMNS.map(values::get).toTypedArray())
        }
}
