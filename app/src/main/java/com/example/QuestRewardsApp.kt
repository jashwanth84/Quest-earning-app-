package com.example

import android.app.Application
import androidx.room.Room
import com.example.ads.AdManager
import com.example.auth.AuthService
import com.example.data.local.AppDatabase
import com.example.data.remote.FirestoreRepository
import com.example.notifications.NotificationHelper

class QuestRewardsApp : Application() {

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

        // Initialize Room Database
        appDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "quest_rewards.db"
        ).fallbackToDestructiveMigration().build()

        // Initialize Repository
        firestoreRepository = FirestoreRepository(localDb = appDatabase)

        // Initialize AdManager
        adManager = AdManager(applicationContext)

        // Initialize AuthService
        authService = AuthService(
            context = applicationContext,
            localDb = appDatabase,
            firestoreRepository = firestoreRepository
        )

        // Create Notification Channel for high-priority notifications
        NotificationHelper.createNotificationChannel(applicationContext)
    }

    companion object {
        lateinit var instance: QuestRewardsApp
            private set
    }
}
