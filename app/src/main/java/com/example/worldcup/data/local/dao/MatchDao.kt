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

    // Matches on a specific local day (pass start/end of day as epoch ms)
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

    // ── API match-lookup queries ────────────────────────────────────────────

    /**
     * Primary lookup: exact kickoff time + home team ID (TLA).
     * Correctly disambiguates simultaneous matches (e.g. group stage matchday 3).
     */
    @Query("SELECT * FROM matches WHERE kickoffTimeMs = :kickoffMs AND homeTeamId = :homeTeamTla LIMIT 1")
    suspend fun findMatchByKickoffAndTeam(kickoffMs: Long, homeTeamTla: String): MatchEntity?

    /**
     * Fallback: match within ±3 minutes.
     * Catches cases where the API's kickoff time is slightly off from our seed data,
     * or where TLA codes differ (e.g. football-data.org uses "RSA", we use "ZAF").
     */
    @Query("SELECT * FROM matches WHERE ABS(kickoffTimeMs - :kickoffMs) <= 180000 LIMIT 1")
    suspend fun findMatchByApproxKickoff(kickoffMs: Long): MatchEntity?

    /**
     * Completed matches for a group as a Flow — used for reactive standings computation.
     * Emits a new list whenever any match result in the group changes.
     */
    @Transaction
    @Query("SELECT * FROM matches WHERE groupId = :groupId AND status = 'COMPLETED' ORDER BY kickoffTimeMs ASC")
    fun getCompletedMatchesForGroup(groupId: String): Flow<List<MatchWithTeams>>

    /**
     * Returns matches on a given day that kicked off before [cutoffMs] and are
     * still not COMPLETED. A non-empty result means a finished-matches API call
     * is warranted.
     */
    @Query("""
        SELECT * FROM matches
        WHERE kickoffTimeMs >= :startOfDayMs
          AND kickoffTimeMs < :endOfDayMs
          AND kickoffTimeMs <= :cutoffMs
          AND status != 'COMPLETED'
    """)
    suspend fun getMatchesNeedingFinishedUpdate(
        startOfDayMs: Long,
        endOfDayMs: Long,
        cutoffMs: Long,
    ): List<MatchEntity>
}
