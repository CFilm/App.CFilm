package com.example.peliselprimazo.data.source

import com.example.peliselprimazo.data.repository.RemoteUserData

interface RemoteDatabaseService {
    suspend fun isUserVip(userId: String): Boolean
    suspend fun getTrends(): List<Int>
    suspend fun fetchRemoteUserData(userId: String): RemoteUserData?
    suspend fun updateLiked(userId: String, movieId: Int, isLiked: Boolean)
    suspend fun updateWatchLater(userId: String, movieId: Int, isWatchLater: Boolean)
    suspend fun updateSaved(userId: String, movieId: Int, isSaved: Boolean)
    suspend fun updateFinished(userId: String, movieId: Int, isFinished: Boolean)
    suspend fun addToWatching(userId: String, movieId: Int, timestamp: Long)
    suspend fun updatePlaybackProgress(userId: String, movieId: Int, position: Long, duration: Long, timestamp: Long)
    suspend fun reportBrokenLink(movieId: Int, serverName: String, fileId: String, userId: String)
    suspend fun createUserData(userId: String, username: String, email: String)
    suspend fun updateUserAvatar(userId: String, uri: String)
}

class NoOpRemoteDatabaseService : RemoteDatabaseService {
    override suspend fun isUserVip(userId: String): Boolean = false
    override suspend fun getTrends(): List<Int> = emptyList()
    override suspend fun fetchRemoteUserData(userId: String): RemoteUserData? = null
    override suspend fun updateLiked(userId: String, movieId: Int, isLiked: Boolean) {}
    override suspend fun updateWatchLater(userId: String, movieId: Int, isWatchLater: Boolean) {}
    override suspend fun updateSaved(userId: String, movieId: Int, isSaved: Boolean) {}
    override suspend fun updateFinished(userId: String, movieId: Int, isFinished: Boolean) {}
    override suspend fun addToWatching(userId: String, movieId: Int, timestamp: Long) {}
    override suspend fun updatePlaybackProgress(userId: String, movieId: Int, position: Long, duration: Long, timestamp: Long) {}
    override suspend fun reportBrokenLink(movieId: Int, serverName: String, fileId: String, userId: String) {}
    override suspend fun createUserData(userId: String, username: String, email: String) {}
    override suspend fun updateUserAvatar(userId: String, uri: String) {}
}
