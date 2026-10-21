package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.local.AppDatabase
import com.example.data.remote.FirestoreRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthService(
    private val context: Context,
    private val localDb: AppDatabase,
    private val firestoreRepository: FirestoreRepository
) {
    private val tag = "AuthService"
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    // Web client ID from google-services.json
    val webClientId = "206042227392-neai3il3b8g7bjvel6hug1hvhjgmprud.apps.googleusercontent.com"

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signInWithGoogle(activityContext: Context): Result<FirebaseUser> = runCatching {
        val rawNonce = UUID.randomUUID().toString()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(rawNonce.toByteArray())
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Avoid double account pickers
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result: GetCredentialResponse = try {
            credentialManager.getCredential(
                request = request,
                context = activityContext
            )
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (e: GetCredentialException) {
            Log.e(tag, "Credential manager error: ${e.message}", e)
            throw e
        }

        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user was null after sign in")

            // Initialize or sync user record in Firestore and Room
            firestoreRepository.initOrSyncUser(
                uid = user.uid,
                displayName = user.displayName ?: "Quest Gamer",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: ""
            )

            // Register FCM token and subscribe to "all_users"
            registerFcmToken(user.uid)

            user
        } else {
            throw IllegalStateException("Unexpected credential type: ${credential.javaClass.name}")
        }
    }

    suspend fun registerFcmToken(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            firestoreRepository.updateFcmToken(uid, token)
            FirebaseMessaging.getInstance().subscribeToTopic("all_users").await()
            Log.d(tag, "Registered FCM token and subscribed to all_users topic")
        } catch (e: Exception) {
            Log.w(tag, "Failed to register FCM token or topic: ${e.message}")
        }
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrEmpty()) {
            try {
                firestoreRepository.clearFcmToken(uid)
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users").await()
                FirebaseMessaging.getInstance().deleteToken().await()
            } catch (e: Exception) {
                Log.w(tag, "Error during FCM cleanup on logout: ${e.message}")
            }
        }
        // Clear local balance database cleanly
        localDb.userBalanceDao().clear()

        // Firebase sign out
        auth.signOut()
    }
}
