package com.example.peliselprimazo.data.source

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRemoteAuthService(
    private val firebaseAuth: FirebaseAuth
) : RemoteAuthService {

    override val currentUser: Flow<RemoteUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val u = auth.currentUser
            if (u != null) {
                trySend(RemoteUser(u.uid, u.displayName, u.email, u.photoUrl?.toString()))
            } else {
                trySend(null)
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        trySend(
            firebaseAuth.currentUser?.let { u ->
                RemoteUser(u.uid, u.displayName, u.email, u.photoUrl?.toString())
            }
        )
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    override suspend fun register(username: String, email: String, password: String): RemoteUser {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Error al crear usuario")
        
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()
        firebaseUser.updateProfile(profileUpdates).await()

        return RemoteUser(
            uid = firebaseUser.uid,
            displayName = username,
            email = email,
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }

    override suspend fun login(email: String, password: String): Boolean {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun updateAvatar(uri: String) {
        val firebaseUser = firebaseAuth.currentUser ?: return
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(Uri.parse(uri))
            .build()
        firebaseUser.updateProfile(profileUpdates).await()
    }
}
