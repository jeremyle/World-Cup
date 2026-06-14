package com.example.worldcup.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.worldcup.data.local.entity.MatchEntity
import com.example.worldcup.data.local.entity.MatchWithTeams
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(matches: List<MatchEntity>)

    @Update
    suspend fun update(match: MatchEntity)

    // All matches with joined team/stadium data, ordered by kickoff
    @Transaction
    @Query("SELECT * FROM matches ORDER BY kickoffTimeMs ASC")
    fun getAllMatchesWithTeams(): Flow<List<MatchWithTeams>>

    // Matches on a specific UTC day (pass start/end of day as epoch ms)
    @Transaction
    @Query("""
        SELECT * FROM matches
        WHERE kickoffTimeMs >= :startOfDayMs AND kickoffTimeMs < :endOfDayMs
        ORDER BY kickoffTimeMs ASC
    """)
    fun getMatchesForDay(startOfDayMs: Long, endOfDayMs: Long): Flow<List<MatchWithTeams>>

    // Currently live matches
    @Transaction
    @Query("SELECT * FROM matches WHERE status = 'LIVE' ORDER BY kickoffTimeMs ASC")
    fun getLiveMatches(): Flow<List<MatchWithTeams>>

    // Matches by group
    @Transaction
    @Query("""
        SELECT * FROM matches
        WHERE groupId = :groupId
        ORDER BY matchday ASC, kickoffTimeMs ASC
    """)
    fun getMatchesByGroup(groupId: String): Flow<List<MatchWithTeams>>

    // Update score + status from API
    @Query("""
        UPDATE matches
        SET homeScore = :homeScore,
            awayScore = :awayScore,
            status = :status,
            minute = :minute
        WHERE id = :matchId
    """)
    suspend fun updateScore(matchId: String, homeScore: Int, awayScore: Int, status: String, minute: Int?)

    @Query("SELECT COUNT(*) FROM matches")
    suspend fun count(): Int
}
