package com.terinit.rhythmicreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        RecoverySessionEntity::class,
        QualifiedPageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun recoveryDao(): RecoveryDao
}
