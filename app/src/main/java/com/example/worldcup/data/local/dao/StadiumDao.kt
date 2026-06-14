package com.example.worldcup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.worldcup.data.local.entity.StadiumEntity

@Dao
interface StadiumDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stadiums: List<StadiumEntity>)

    @Query("SELECT * FROM stadiums ORDER BY country, city")
    suspend fun getAllStadiums(): List<StadiumEntity>

    @Query("SELECT COUNT(*) FROM stadiums")
    suspend fun count(): Int
}
