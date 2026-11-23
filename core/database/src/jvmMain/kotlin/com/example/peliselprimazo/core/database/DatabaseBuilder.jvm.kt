package com.example.peliselprimazo.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

fun getDatabase(): PelisDatabase {
    val dbFile = File(System.getProperty("java.io.tmpdir"), PelisDatabase.DATABASE_NAME)
    return Room.databaseBuilder<PelisDatabase>(
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}
