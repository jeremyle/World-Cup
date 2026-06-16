package com.example.worldcup.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.worldcup.data.local.entity.PlayerStatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatDao {

    @Upsert
    suspend fun upsertAll(stats: List<PlayerStatEntity>)

    /** Top scorers ranked by goals descending. */
    @Query("SELECT * FROM player_stats WHERE statType = 'SCORER' ORDER BY rank ASC")
    fun getTopScorers(): Flow<List<PlayerStatEntity>>

    /** Top assists ranked by assists descending. */
    @Query("SELECT * FROM player_stats WHERE statType = 'ASSISTER' ORDER BY rank ASC")
    fun getTopAssists(): Flow<List<PlayerStatEntity>>
}
