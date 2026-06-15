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
     * Catches cases where the API's kickoff time is slightly off from our seed data.
     */
    @Query("SELECT * FROM matches WHERE ABS(kickoffTimeMs - :kickoffMs) <= 180000 LIMIT 1")
    suspend fun findMatchByApproxKickoff(kickoffMs: Long): MatchEntity?

    /**
     * COMPLETED + LIVE matches for a group — used for reactive standings computation.
     * Emits a new list whenever any match result or status changes.
     * Including LIVE lets standings reflect the current score in real time.
     */
    @Transaction
    @Query("""
        SELECT * FROM matches
        WHERE groupId = :groupId
          AND status IN ('COMPLETED', 'LIVE')
        ORDER BY kickoffTimeMs ASC
    """)
    fun getPlayedMatchesForGroup(groupId: String): Flow<List<MatchWithTeams>>

    /** Count of currently LIVE matches in [groupId]. Used to decide whether to keep polling. */
    @Query("SELECT COUNT(*) FROM matches WHERE groupId = :groupId AND status = 'LIVE'")
    suspend fun countLiveMatchesInGroup(groupId: String): Int

    /**
     * All past matches (kicked off > 110 min ago) not yet marked COMPLETED.
     * Non-empty result → a bulk FINISHED fetch from the API is warranted.
     * LIVE matches are included so a match stuck in LIVE from a prior session
     * gets updated if the server now reports it as FINISHED.
     */
    @Query("SELECT * FROM matches WHERE kickoffTimeMs <= :cutoffMs AND status != 'COMPLETED'")
    suspend fun getPendingFinishedMatches(cutoffMs: Long): List<MatchEntity>
}
