package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        ChildProfileEntity::class,
        AppRuleEntity::class,
        ScheduleEntity::class,
        MessageEntity::class,
        TimeRequestEntity::class,
        SafeZoneEntity::class,
        LocationHistoryEntity::class,
        AlertEntity::class,
        UsedNonceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WahjDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun childDao(): ChildDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun messageDao(): MessageDao
    abstract fun timeRequestDao(): TimeRequestDao
    abstract fun safeZoneDao(): SafeZoneDao
    abstract fun locationHistoryDao(): LocationHistoryDao
    abstract fun alertDao(): AlertDao
    abstract fun usedNonceDao(): UsedNonceDao

    companion object {
        @Volatile
        private var INSTANCE: WahjDatabase? = null

        fun getInstance(context: Context): WahjDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WahjDatabase::class.java,
                    "wahj_family.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
