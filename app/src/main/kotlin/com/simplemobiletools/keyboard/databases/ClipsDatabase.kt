package com.simplemobiletools.keyboard.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.simplemobiletools.keyboard.interfaces.ClipsDao
import com.simplemobiletools.keyboard.models.Clip

@Database(entities = [Clip::class], version = 1)
abstract class ClipsDatabase : RoomDatabase() {

    abstract fun ClipsDao(): ClipsDao

    companion object {
        private var db: ClipsDatabase? = null

        fun getInstance(context: Context): ClipsDatabase {
            if (db == null) {
                synchronized(ClipsDatabase::class) {
                    if (db == null) {
                        db = Room.databaseBuilder(context.applicationContext, ClipsDatabase::class.java, "clips.db").build()
                        db!!.openHelper.setWriteAheadLoggingEnabled(true)
                    }
                }
            }
            return db!!
        }

        fun destroyInstance() {
            db = null
        }
    }
}
