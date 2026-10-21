package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_balance")
data class UserBalanceEntity(
    @PrimaryKey val uid: String,
    val coins: Long,
    val gems: Long,
    val displayName: String,
    val email: String,
    val photoUrl: String,
    val updatedAt: Long
)

@Entity(tableName = "notification_read")
data class NotificationReadEntity(
    @PrimaryKey val notificationId: String,
    val readAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_history")
data class TaskHistoryEntity(
    @PrimaryKey val taskId: String,
    val title: String,
    val rewardCoins: Long,
    val category: String,
    val completedAt: Long = System.currentTimeMillis()
)

@Dao
interface UserBalanceDao {
    @Query("SELECT * FROM user_balance WHERE uid = :uid LIMIT 1")
    fun getBalance(uid: String): Flow<UserBalanceEntity?>

    @Query("SELECT * FROM user_balance WHERE uid = :uid LIMIT 1")
    suspend fun getBalanceOnce(uid: String): UserBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBalance(balance: UserBalanceEntity)

    @Query("DELETE FROM user_balance")
    suspend fun clear()
}

@Dao
interface NotificationReadDao {
    @Query("SELECT notificationId FROM notification_read")
    fun getReadNotificationIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAsRead(read: NotificationReadEntity)

    @Query("DELETE FROM notification_read")
    suspend fun clear()
}

@Dao
interface TaskHistoryDao {
    @Query("SELECT * FROM task_history ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordCompletion(task: TaskHistoryEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM task_history WHERE taskId = :taskId)")
    suspend fun isTaskCompleted(taskId: String): Boolean

    @Query("DELETE FROM task_history")
    suspend fun clear()
}

@Database(
    entities = [
        UserBalanceEntity::class,
        NotificationReadEntity::class,
        TaskHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userBalanceDao(): UserBalanceDao
    abstract fun notificationReadDao(): NotificationReadDao
    abstract fun taskHistoryDao(): TaskHistoryDao
}
