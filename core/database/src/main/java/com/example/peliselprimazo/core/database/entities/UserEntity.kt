package com.example.peliselprimazo.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val email: String,
    val password: String,
    val profilePictureUri: String? = null,
    val isLoggedIn: Boolean = false
)
