package com.example

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.ads.AdManager
import com.example.auth.AuthService
import com.example.data.local.AppDatabase
import com.example.data.remote.FirestoreRepository
import com.example.notifications.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class QuestRewardsApp : Application() {

    private val tag = "QuestRewardsApp"

    lateinit var appDatabase: AppDatabase
        private set

    lateinit var firestoreRepository: FirestoreRepository
        private set

    lateinit var adManager: AdManager
        private set

    lateinit var authService: AuthService
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Firebase explicitly before any Firebase SDK call
        initFirebaseSafely()

        // 2. Initialize Room Database with fallback to destructive migration
        try {
            appDatabase = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "quest_rewards.db"
            ).fallbackToDestructiveMigration().build()
        } catch (e: Throwable) {
            Log.e(tag, "Room database initialization error: ${e.message}", e)
            // Re-attempt with in-memory fallback if disk DB is corrupted
            appDatabase = Room.inMemoryDatabaseBuilder(
                applicationContext,
                AppDatabase::class.java
            ).build()
        }

        // 3. Initialize Repository
        firestoreRepository = FirestoreRepository(localDb = appDatabase)

        // 4. Initialize AdManager
        adManager = AdManager(applicationContext)

        // 5. Initialize AuthService
        authService = AuthService(
            context = applicationContext,
            localDb = appDatabase,
            firestoreRepository = firestoreRepository
        )

        // 6. Create Notification Channel for high-priority notifications
        try {
            NotificationHelper.createNotificationChannel(applicationContext)
        } catch (e: Throwable) {
            Log.e(tag, "Notification channel creation error: ${e.message}", e)
        }
    }

    private fun initFirebaseSafely() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val app = FirebaseApp.initializeApp(this)
                if (app == null) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:206042227392:android:ec3f5fb60e46b7898c8325")
                        .setProjectId("quest-rewards-6fc5f")
                        .setApiKey("AIzaSyDqWsE7skObBWV1uUJBY6eKZvej_6-8imA")
                        .setStorageBucket("quest-rewards-6fc5f.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.d(tag, "Firebase initialized with explicit FirebaseOptions")
                } else {
                    Log.d(tag, "Firebase initialized successfully with default provider")
                }
            } else {
                Log.d(tag, "Firebase already initialized")
            }
        } catch (e: Throwable) {
            Log.w(tag, "Default Firebase init threw: ${e.message}, attempting explicit options", e)
            try {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:206042227392:android:ec3f5fb60e46b7898c8325")
                    .setProjectId("quest-rewards-6fc5f")
                    .setApiKey("AIzaSyDqWsE7skObBWV1uUJBY6eKZvej_6-8imA")
                    .setStorageBucket("quest-rewards-6fc5f.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d(tag, "Firebase initialized via fallback options")
            } catch (fallbackError: Throwable) {
                Log.e(tag, "Fatal: Firebase could not be initialized: ${fallbackError.message}", fallbackError)
            }
        }
    }

    companion object {
        lateinit var instance: QuestRewardsApp
            private set
    }
}
