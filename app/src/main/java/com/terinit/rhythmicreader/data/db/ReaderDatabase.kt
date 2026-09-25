package com.terinit.rhythmicreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookEntity::class,
        RecoverySessionEntity::class,
        QualifiedPageEntity::class,
        DailyReadingEvidenceEntity::class,
        DailyQualifiedPageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun recoveryDao(): RecoveryDao
    abstract fun dailyReadingEvidenceDao(): DailyReadingEvidenceDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recovery_sessions ADD COLUMN protocolVersion INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_reading_evidence (
                        dateKey TEXT NOT NULL PRIMARY KEY,
                        verifiedActiveSeconds INTEGER NOT NULL,
                        updatedAtEpochMs INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_qualified_pages (
                        dateKey TEXT NOT NULL,
                        bookId TEXT NOT NULL,
                        pageIndex INTEGER NOT NULL,
                        qualifiedAtEpochMs INTEGER NOT NULL,
                        PRIMARY KEY(dateKey, bookId, pageIndex)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
