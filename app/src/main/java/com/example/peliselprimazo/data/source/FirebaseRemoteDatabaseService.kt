package com.example.peliselprimazo.data.source

import com.example.peliselprimazo.data.repository.RemoteUserData
import com.example.peliselprimazo.data.repository.RemoteWatchingProgress
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirebaseRemoteDatabaseService(
    private val firestore: FirebaseFirestore
) : RemoteDatabaseService {

    override suspend fun isUserVip(userId: String): Boolean {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            doc.getBoolean("isVip") ?: false
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getTrends(): List<Int> {
        return try {
            val snapshot = firestore.collection("trends")
                .orderBy("count", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            snapshot.documents.mapNotNull { it.id.toIntOrNull() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchRemoteUserData(userId: String): RemoteUserData? {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val liked = (doc.get("liked") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                val saved = (doc.get("saved") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                val watchLater = (doc.get("watchLater") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                val finished = (doc.get("finished") as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                
                val watchingMap = doc.get("watching") as? Map<String, Map<String, Any>> ?: emptyMap()
                val watching = watchingMap.mapNotNull { (idStr, data) ->
                    val id = idStr.toIntOrNull() ?: return@mapNotNull null
                    id to RemoteWatchingProgress(
                        position = (data["position"] as? Long) ?: 0L,
                        duration = (data["duration"] as? Long) ?: 0L,
                        timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis()
                    )
                }.toMap()

                RemoteUserData(liked, saved, watchLater, finished, watching)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun updateLiked(userId: String, movieId: Int, isLiked: Boolean) {
        val userRef = firestore.collection("users").document(userId)
        if (isLiked) {
            userRef.update("liked", FieldValue.arrayUnion(movieId)).await()
            firestore.collection("trends").document(movieId.toString())
                .set(mapOf("count" to FieldValue.increment(1)), SetOptions.merge())
        } else {
            userRef.update("liked", FieldValue.arrayRemove(movieId)).await()
            firestore.collection("trends").document(movieId.toString())
                .set(mapOf("count" to FieldValue.increment(-1)), SetOptions.merge())
        }
    }

    override suspend fun updateWatchLater(userId: String, movieId: Int, isWatchLater: Boolean) {
        val userRef = firestore.collection("users").document(userId)
        if (isWatchLater) {
            userRef.update("watchLater", FieldValue.arrayUnion(movieId)).await()
        } else {
            userRef.update("watchLater", FieldValue.arrayRemove(movieId)).await()
        }
    }

    override suspend fun updateSaved(userId: String, movieId: Int, isSaved: Boolean) {
        val userRef = firestore.collection("users").document(userId)
        if (isSaved) {
            userRef.update("saved", FieldValue.arrayUnion(movieId)).await()
        } else {
            userRef.update("saved", FieldValue.arrayRemove(movieId)).await()
        }
    }

    override suspend fun updateFinished(userId: String, movieId: Int, isFinished: Boolean) {
        val userRef = firestore.collection("users").document(userId)
        if (isFinished) {
            userRef.update("finished", FieldValue.arrayUnion(movieId)).await()
        } else {
            userRef.update("finished", FieldValue.arrayRemove(movieId)).await()
        }
    }

    override suspend fun addToWatching(userId: String, movieId: Int, timestamp: Long) {
        val userRef = firestore.collection("users").document(userId)
        userRef.update("watching.$movieId.timestamp", timestamp).await()
        firestore.collection("trends").document(movieId.toString())
            .set(mapOf("views" to FieldValue.increment(1)), SetOptions.merge())
    }

    override suspend fun updatePlaybackProgress(userId: String, movieId: Int, position: Long, duration: Long, timestamp: Long) {
        val userRef = firestore.collection("users").document(userId)
        val data = mapOf(
            "watching.$movieId.position" to position,
            "watching.$movieId.duration" to duration,
            "watching.$movieId.timestamp" to timestamp
        )
        userRef.update(data)
    }

    override suspend fun reportBrokenLink(movieId: Int, serverName: String, fileId: String, userId: String) {
        val report = mapOf(
            "movieId" to movieId,
            "server" to serverName,
            "fileId" to fileId,
            "timestamp" to System.currentTimeMillis(),
            "userId" to userId
        )
        firestore.collection("reports").add(report).await()
    }

    override suspend fun createUserData(userId: String, username: String, email: String) {
        val userData = mapOf(
            "username" to username,
            "email" to email,
            "createdAt" to System.currentTimeMillis(),
            "liked" to emptyList<Int>(),
            "saved" to emptyList<Int>(),
            "watchLater" to emptyList<Int>(),
            "finished" to emptyList<Int>(),
            "watching" to emptyMap<String, Any>()
        )
        firestore.collection("users").document(userId).set(userData).await()
    }

    override suspend fun updateUserAvatar(userId: String, uri: String) {
        firestore.collection("users").document(userId)
            .update("profilePictureUri", uri).await()
    }
}
