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
     * Returns `true` if any match was updated (score / status changed), `false` otherwise.
     * Callers can use this to decide whether to refresh downstream stats (e.g. player stats).
     */
    suspend fun refreshAllFinishedMatches(): Boolean {
        val cutoffMs = Clock.System.now().toEpochMilliseconds() - 110L * 60 * 1000
        val pending  = matchDao.getPendingFinishedMatches(cutoffMs)

        if (pending.isEmpty()) {
            Log.d(TAG, "refreshAllFinishedMatches: nothing pending, skipping API")
            return false
        }

        Log.d(TAG, "refreshAllFinishedMatches: ${pending.size} pending match(es), fetching from API")
        val today    = Clock.System.todayIn(TimeZone.UTC).toString()
        val response = api.getMatches(
            status   = "FINISHED",
            dateFrom = WC_START_DATE,
            dateTo   = today,
        )

        var anyUpdated = false
        response.matches.forEach { dto ->
            if (updateMatchInDb(dto)) anyUpdated = true
        }
        Log.d(TAG, "refreshAllFinishedMatches: processed ${response.matches.size} match(es), updated=$anyUpdated")
        return anyUpdated
    }

    /**
     * Fetches all currently LIVE matches from the API and updates the DB.
     * Returns `true` if any score or status changed.
     */
    suspend fun refreshLiveMatches(): Boolean {
        val response = api.getMatches(status = "LIVE")
        if (response.matches.isEmpty()) return false

        Log.d(TAG, "refreshLiveMatches: ${response.matches.size} live match(es)")
        var anyUpdated = false
        response.matches.forEach { dto ->
            if (updateMatchInDb(dto)) anyUpdated = true
        }
        return anyUpdated
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Looks up the local DB entity for [dto], compares score/status,
     * and only writes to DB if something actually changed.
     * Returns `true` if the DB was written.
     */
    private suspend fun updateMatchInDb(dto: MatchDto): Boolean {
        val kickoffMs = Instant.parse(dto.utcDate).toEpochMilliseconds()
        val tla       = dto.homeTeam.tla

        val entity =
            (if (tla != null) matchDao.findMatchByKickoffAndTeam(kickoffMs, tla) else null)
                ?: matchDao.findMatchByApproxKickoff(kickoffMs)

        if (entity == null) {
            Log.w(TAG, "No local match for ${dto.homeTeam.name} vs ${dto.awayTeam.name} at ${dto.utcDate}")
            return false
        }

        val newStatus = when (dto.status) {
            "IN_PLAY", "LIVE", "PAUSED" -> "LIVE"
            "FINISHED"                  -> "COMPLETED"
            else                        -> return false
        }

        val newHome = dto.score.fullTime.home ?: 0
        val newAway = dto.score.fullTime.away ?: 0

        if (entity.status    == newStatus &&
            entity.homeScore == newHome   &&
            entity.awayScore == newAway   &&
            entity.minute    == dto.minute) {
            return false
        }

        Log.d(TAG, "updateMatchInDb: ${dto.homeTeam.name} $newHome–$newAway ${dto.awayTeam.name} [$newStatus]")
        matchDao.updateScore(entity.id, newHome, newAway, newStatus, dto.minute)
        return true
    }

    companion object {
        private const val TAG           = "MatchRepository"
        private const val WC_START_DATE = "2026-06-11"
    }
}
