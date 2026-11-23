package com.example.peliselprimazo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.peliselprimazo.core.database.dao.MovieDao
import com.example.peliselprimazo.core.database.dao.SearchHistoryDao
import com.example.peliselprimazo.core.database.dao.UserContentDao
import com.example.peliselprimazo.core.database.dao.UserDao
import com.example.peliselprimazo.core.database.entities.MovieEntity
import com.example.peliselprimazo.core.database.entities.SearchHistoryEntity
import com.example.peliselprimazo.core.database.entities.UserContentEntity
import com.example.peliselprimazo.core.database.entities.UserEntity

@Database(
    entities = [UserContentEntity::class, MovieEntity::class, UserEntity::class, SearchHistoryEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PelisDatabase : RoomDatabase() {
    abstract val userContentDao: UserContentDao
    abstract val movieDao: MovieDao
    abstract val userDao: UserDao
    abstract val searchHistoryDao: SearchHistoryDao

    companion object {
        const val DATABASE_NAME = "pelis_el_primazo_db"
    }
}
