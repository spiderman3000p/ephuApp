package com.tau.ephuapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tau.ephuapp.database.daos.*
import com.tau.ephuapp.models.*

@Database(entities = [Task::class, FetchedDataHistory::class,
    Location::class, Device::class, Item::class, ItemCount::class, TaskParameter::class,
    ItemCountTask::class, DeliveryLine::class, Certification::class, CertificationTaskItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tasksDao(): TaskDao
    abstract fun tasksParameterDao(): TaskParameterDao
    abstract fun taskLocationsDao(): TaskLocationDao
    abstract fun certificationTaskItemsDao(): CertificationTaskItemDao
    abstract fun certificationsDao(): CertificationDao
    abstract fun itemDao(): ItemDao
    abstract fun itemCountDao(): ItemCountDao
    abstract fun itemCountTaskDao(): ItemCountTaskDao
    abstract fun deviceDao(): DeviceDao
    abstract fun fetchedHistoryDao(): FetchedDataHistoryDao
    abstract fun deliveryLineDao(): DeliveryLineDao
    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ephuapp_database"
                )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}