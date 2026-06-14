package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LogoEntity::class], version = 1, exportSchema = false)
@TypeConverters(LogoConverters::class)
abstract class LogoDatabase : RoomDatabase() {
    abstract fun logoDao(): LogoDao

    companion object {
        @Volatile
        private var INSTANCE: LogoDatabase? = null

        fun getDatabase(context: Context): LogoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogoDatabase::class.java,
                    "logo_studio_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
