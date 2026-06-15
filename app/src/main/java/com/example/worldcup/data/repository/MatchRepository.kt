package com.example.worldcup.data.repository

import android.util.Log
import com.example.worldcup.data.local.dao.MatchDao
import com.example.worldcup.data.remote.FootballDataApi
import com.example.worldcup.data.remote.FootballDataApiClient
import com.example.worldcup.data.remote.dto.MatchDto
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class MatchRepository(
    private val matchDao: MatchDao,
    private val api: FootballDataApi = FootballDataApiClient.api,
) {

    /**
     * Fetches all FINISHED World Cup matches from the tournament start date up to today
     * and persists them to the DB.
     *
     * Skips the API call entirely if there are no past matches still waiting for a final
     * score — i.e., every match that should be done is already COMPLETED in the DB.
     * This makes finished-match data a permanent cache: once a score is stored it is
     * never re-fetched, but a 0-0 COMPLETED result is correctly preserved (status
     * distinguishes it from an unplayed UPCOMING match).
     */
    suspend fun refreshAllFinishedMatches() {
        val cutoffMs = Clock.System.now().toEpochMilliseconds() - 110L * 60 * 1000
        val pending  = matchDao.getPendingFinishedMatches(cutoffMs)

        if (pending.isEmpty()) {
            Log.d(TAG, "refreshAllFinishedMatches: nothing pending, skipping API")
            return
        }

        Log.d(TAG, "refreshAllFinishedMatches: ${pending.size} pending match(es), fetching from API")
        val today    = Clock.System.todayIn(TimeZone.UTC).toString()
        val response = api.getMatches(
            status   = "FINISHED",
            dateFrom = WC_START_DATE,
            dateTo   = today,
        )
        response.matches.forEach { updateMatchInDb(it) }
        Log.d(TAG, "refreshAllFinishedMatches: processed ${response.matches.size} finished match(es)")
    }

    /**
     * Fetches all currently LIVE matches from the API and updates the DB.
     * The Room Flow on [MatchDao.getPlayedMatchesForGroup] will re-emit automatically
     * when any row changes, triggering a standings recomputation in [GroupRepository].
     */
    suspend fun refreshLiveMatches() {
        val response = api.getMatches(status = "LIVE")
        if (response.matches.isEmpty()) return
        Log.d(TAG, "refreshLiveMatches: ${response.matches.size} live match(es)")
        response.matches.forEach { updateMatchInDb(it) }
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Looks up the local DB entity for [dto], compares score/status,
     * and only writes to DB if something actually changed.
     */
    private suspend fun updateMatchInDb(dto: MatchDto) {
        val kickoffMs = Instant.parse(dto.utcDate).toEpochMilliseconds()
        val tla       = dto.homeTeam.tla

        val entity =
            (if (tla != null) matchDao.findMatchByKickoffAndTeam(kickoffMs, tla) else null)
                ?: matchDao.findMatchByApproxKickoff(kickoffMs)

        if (entity == null) {
            Log.w(TAG, "No local match for ${dto.homeTeam.name} vs ${dto.awayTeam.name} at ${dto.utcDate}")
            return
        }

        val newStatus = when (dto.status) {
            "IN_PLAY", "LIVE", "PAUSED" -> "LIVE"
            "FINISHED"                  -> "COMPLETED"
            else                        -> return
        }

        val newHome = dto.score.fullTime.home ?: 0
        val newAway = dto.score.fullTime.away ?: 0

        // Skip DB write if nothing changed — avoids spurious Room Flow emissions
        if (entity.status    == newStatus &&
            entity.homeScore == newHome   &&
            entity.awayScore == newAway   &&
            entity.minute    == dto.minute) {
            return
        }

        Log.d(TAG, "updateMatchInDb: ${dto.homeTeam.name} $newHome–$newAway ${dto.awayTeam.name} [$newStatus]")
        matchDao.updateScore(entity.id, newHome, newAway, newStatus, dto.minute)
    }

    companion object {
        private const val TAG           = "MatchRepository"
        private const val WC_START_DATE = "2026-06-11"
    }
}
