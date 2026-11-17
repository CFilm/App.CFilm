package com.example.peliselprimazo.core.model

data class User(
    val username: String = "",
    val email: String = "",
    val profilePictureUri: String? = null,
    val isLoggedIn: Boolean = false
)
