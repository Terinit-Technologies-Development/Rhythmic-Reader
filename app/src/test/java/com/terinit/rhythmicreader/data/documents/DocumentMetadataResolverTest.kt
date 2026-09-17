package com.terinit.rhythmicreader.data.documents

import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DocumentMetadataResolverTest {

    @Test
    fun getDisplayName_returnsNameWhenAvailable() {
        val resolver = mock(ContentResolver::class.java)
        val uri = Uri.parse("content://com.test.provider/document/1")

        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf("Deep Work.pdf"))

        `when`(resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null))
            .thenReturn(cursor)

        val name = resolver.getDisplayName(uri)
        assertEquals("Deep Work.pdf", name)
    }

    @Test
    fun getDisplayName_fallsBackWhenCursorIsEmpty() {
        val resolver = mock(ContentResolver::class.java)
        val uri = Uri.parse("content://com.test.provider/document/fallback_file.pdf")

        val emptyCursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
        `when`(resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null))
            .thenReturn(emptyCursor)

        val name = resolver.getDisplayName(uri)
        assertEquals("fallback_file.pdf", name)
    }

    @Test
    fun getDisplayName_fallsBackToUntitledPdfWhenUriHasNoSegment() {
        val resolver = mock(ContentResolver::class.java)
        val uri = Uri.parse("content://empty")

        val name = resolver.getDisplayName(uri)
        assertEquals("Untitled PDF", name)
    }

    @Test
    fun getFileSize_returnsCorrectBytes() {
        val resolver = mock(ContentResolver::class.java)
        val uri = Uri.parse("content://com.test.provider/document/size_test")

        val cursor = MatrixCursor(arrayOf(OpenableColumns.SIZE))
        cursor.addRow(arrayOf(2048576L))

        `when`(resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null))
            .thenReturn(cursor)

        val size = resolver.getFileSize(uri)
        assertEquals(2048576L, size)
    }

    @Test
    fun getFileSize_returnsNullWhenCursorEmpty() {
        val resolver = mock(ContentResolver::class.java)
        val uri = Uri.parse("content://com.test.provider/document/size_empty")

        val emptyCursor = MatrixCursor(arrayOf(OpenableColumns.SIZE))
        `when`(resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null))
            .thenReturn(emptyCursor)

        val size = resolver.getFileSize(uri)
        assertNull(size)
    }
}
