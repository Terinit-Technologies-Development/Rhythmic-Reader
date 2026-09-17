package com.terinit.rhythmicreader.data.db

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecoveryMigrationTest {

    @Test
    fun migration_2_to_3_addsProtocolVersionColumnAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS recovery_sessions (
                            sessionId TEXT PRIMARY KEY NOT NULL,
                            requiredActiveSeconds INTEGER NOT NULL,
                            requiredQualifiedPages INTEGER NOT NULL,
                            accumulatedActiveMs INTEGER NOT NULL,
                            status TEXT NOT NULL,
                            createdAtEpochMs INTEGER NOT NULL,
                            completedAtEpochMs INTEGER,
                            expiresAtEpochMs INTEGER
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Insert Pass 02 legacy data (version 2 schema without protocolVersion)
        db.execSQL(
            """
            INSERT INTO recovery_sessions (
                sessionId, requiredActiveSeconds, requiredQualifiedPages,
                accumulatedActiveMs, status, createdAtEpochMs, completedAtEpochMs, expiresAtEpochMs
            ) VALUES (
                'test-session-v2', 1800, 10, 42000, 'ACTIVE', 100000, NULL, NULL
            )
            """.trimIndent()
        )

        // Execute Migration 2 -> 3
        ReaderDatabase.MIGRATION_2_3.migrate(db)

        // Query migrated schema
        val cursor = db.query("SELECT sessionId, protocolVersion, accumulatedActiveMs FROM recovery_sessions WHERE sessionId = 'test-session-v2'")
        assertTrue(cursor.moveToFirst())
        assertEquals("test-session-v2", cursor.getString(0))
        assertEquals(1, cursor.getInt(1)) // Default value 1
        assertEquals(42000L, cursor.getLong(2)) // Preserved progress
        cursor.close()
        db.close()
    }
}
