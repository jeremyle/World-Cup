package com.example.worldcup.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.worldcup.data.local.dao.MatchDao
import com.example.worldcup.data.local.dao.StadiumDao
import com.example.worldcup.data.local.dao.TeamDao
import com.example.worldcup.data.local.entity.MatchEntity
import com.example.worldcup.data.local.entity.StadiumEntity
import com.example.worldcup.data.local.entity.TeamEntity

@Database(
    entities = [
        TeamEntity::class,
        StadiumEntity::class,
        MatchEntity::class,
    ],
    version = 6,
    exportSchema = false
)
abstract class WorldCupDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao
    abstract fun stadiumDao(): StadiumDao
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile private var INSTANCE: WorldCupDatabase? = null

        fun getInstance(context: Context): WorldCupDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorldCupDatabase::class.java,
                    "worldcup.db"
                )
                .fallbackToDestructiveMigration(true)
                .build().also { INSTANCE = it }
            }
    }
}
