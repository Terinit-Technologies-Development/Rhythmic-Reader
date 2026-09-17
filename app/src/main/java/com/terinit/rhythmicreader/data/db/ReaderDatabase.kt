package com.terinit.rhythmicreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BookEntity::class,
        RecoverySessionEntity::class,
        QualifiedPageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun recoveryDao(): RecoveryDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recovery_sessions ADD COLUMN protocolVersion INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
