package com.example.notifications

import android.util.Log
import com.example.QuestRewardsApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuestFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tag = "QuestFCMService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(tag, "FCM token refreshed: $token")
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val app = application as? QuestRewardsApp
            serviceScope.launch {
                app?.firestoreRepository?.updateFcmToken(currentUser.uid, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(tag, "FCM Message received from: ${remoteMessage.from}")

        // Read notification data or payload
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Quest Rewards"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "You have a new update!"
        val imageUrl = remoteMessage.notification?.imageUrl?.toString() ?: remoteMessage.data["imageUrl"]
        val route = remoteMessage.data["route"] ?: "HOME"

        serviceScope.launch {
            NotificationHelper.showForegroundNotification(
                context = applicationContext,
                title = title,
                body = body,
                imageUrl = imageUrl,
                route = route
            )
        }
    }
}
