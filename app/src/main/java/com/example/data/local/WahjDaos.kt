package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)
}

@Dao
interface ChildDao {
    @Query("SELECT * FROM children WHERE parentId = :parentId ORDER BY pairedAt ASC")
    fun observeByParent(parentId: String): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM children ORDER BY pairedAt ASC")
    fun observeAll(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM children WHERE id = :childId LIMIT 1")
    fun observeById(childId: String): Flow<ChildProfileEntity?>

    @Query("SELECT * FROM children WHERE id = :childId LIMIT 1")
    suspend fun getById(childId: String): ChildProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(child: ChildProfileEntity)

    @Update
    suspend fun update(child: ChildProfileEntity)

    @Query("DELETE FROM children WHERE id = :childId")
    suspend fun deleteById(childId: String)
}

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules WHERE childId = :childId ORDER BY usedTodayMinutes DESC, appName ASC")
    fun observeByChild(childId: String): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules WHERE childId = :childId")
    suspend fun getByChild(childId: String): List<AppRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<AppRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRuleEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE childId = :childId ORDER BY startTime ASC")
    fun observeByChild(childId: String): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE childId = :childId ORDER BY createdAt ASC")
    fun observeByChild(childId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE childId = :childId AND senderRole = :senderRole AND status != 'SCHEDULED'")
    suspend fun markAllStatusForRole(childId: String, senderRole: String, status: String)
}

@Dao
interface TimeRequestDao {
    @Query("SELECT * FROM time_requests WHERE childId = :childId ORDER BY createdAt DESC")
    fun observeByChild(childId: String): Flow<List<TimeRequestEntity>>

    @Query("SELECT * FROM time_requests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TimeRequestEntity>>

    @Query("SELECT * FROM time_requests WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TimeRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: TimeRequestEntity)
}

@Dao
interface SafeZoneDao {
    @Query("SELECT * FROM safe_zones WHERE childId = :childId ORDER BY name ASC")
    fun observeByChild(childId: String): Flow<List<SafeZoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(zone: SafeZoneEntity)

    @Query("DELETE FROM safe_zones WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface LocationHistoryDao {
    @Query("SELECT * FROM location_history WHERE childId = :childId ORDER BY recordedAt DESC LIMIT 30")
    fun observeByChild(childId: String): Flow<List<LocationHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LocationHistoryEntity)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC LIMIT 100")
    fun observeAll(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("UPDATE alerts SET isRead = 1")
    suspend fun markAllRead()
}

@Dao
interface UsedNonceDao {
    @Query("SELECT EXISTS(SELECT 1 FROM used_nonces WHERE nonce = :nonce)")
    suspend fun exists(nonce: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: UsedNonceEntity)
}
