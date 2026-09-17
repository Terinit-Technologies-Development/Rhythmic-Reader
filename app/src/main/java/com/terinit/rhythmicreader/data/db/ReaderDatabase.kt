package com.terinit.rhythmicreader.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
