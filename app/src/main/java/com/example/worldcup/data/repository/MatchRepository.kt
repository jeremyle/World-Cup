package com.example.worldcup.data.repository

import android.util.Log
import com.example.worldcup.data.local.dao.MatchDao
import com.example.worldcup.data.remote.FootballDataApi
import com.example.worldcup.data.remote.FootballDataApiClient
import com.example.worldcup.data.remote.dto.MatchDto
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

class MatchRepository(
    private val matchDao: MatchDao,
    private val api: FootballDataApi = FootballDataApiClient.api,
) {

    /**
     * Fetch FINISHED matches for [date] from the API and persist to DB.
     *
     * Caching logic:
     *  - We check if any match on [date] kicked off more than 110 minutes ago
     *    and is still not COMPLETED in DB.
     *  - If none → all done matches are already cached, skip the API call.
     *  - If some → fetch from API, update DB. Next time this runs for the same
     *    day those matches will be COMPLETED and the check returns empty again.
     *
     * This means a day's finished results are fetched at most once total, ever.
     */
    suspend fun refreshFinishedMatches(date: LocalDate) {
        val zone = TimeZone.currentSystemDefault()
        val startMs = date.atStartOfDayIn(zone).toEpochMilliseconds()
        val endMs   = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
        // A match is considered "should be finished" if it kicked off 110+ min ago
        val cutoffMs = Clock.System.now().toEpochMilliseconds() - 110L * 60 * 1000

        val pending = matchDao.getMatchesNeedingFinishedUpdate(startMs, endMs, cutoffMs)
        if (pending.isEmpty()) {
            Log.d(TAG, "refreshFinishedMatches: all done for $date, skipping API")
            return
        }

        Log.d(TAG, "refreshFinishedMatches: fetching ${pending.size} potentially finished matches for $date")
        val response = api.getMatches(
            status   = "FINISHED",
            dateFrom = date.toString(),
            dateTo   = date.toString(),
        )
        response.matches.forEach { updateMatchInDb(it) }
        Log.d(TAG, "refreshFinishedMatches: updated ${response.matches.size} finished matches")
    }

    /**
     * Fetch all currently LIVE matches and update the DB.
     * Call this on a 60-second polling schedule while any match is in progress.
     */
    suspend fun refreshLiveMatches() {
        val response = api.getMatches(status = "LIVE")
        if (response.matches.isEmpty()) return
        Log.d(TAG, "refreshLiveMatches: ${response.matches.size} live match(es)")
        response.matches.forEach { updateMatchInDb(it) }
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private suspend fun updateMatchInDb(dto: MatchDto) {
        val kickoffMs = Instant.parse(dto.utcDate).toEpochMilliseconds()
        val tla       = dto.homeTeam.tla

        // 1. Try exact kickoff + TLA (handles simultaneous matches correctly)
        // 2. Fallback: approximate kickoff window (handles TLA mismatches like RSA vs ZAF)
        val entity =
            (if (tla != null) matchDao.findMatchByKickoffAndTeam(kickoffMs, tla) else null)
                ?: matchDao.findMatchByApproxKickoff(kickoffMs)

        if (entity == null) {
            Log.w(TAG, "updateMatchInDb: no local match for ${dto.homeTeam.name} vs ${dto.awayTeam.name} at ${dto.utcDate}")
            return
        }

        val newStatus = when (dto.status) {
            "IN_PLAY", "LIVE", "PAUSED" -> "LIVE"
            "FINISHED"                  -> "COMPLETED"
            else                        -> return  // SCHEDULED/TIMED — nothing to update
        }

        Log.d(TAG, "updateMatchInDb: ${dto.homeTeam.name} vs ${dto.awayTeam.name} | status=${dto.status} | minute=${dto.minute}")

        matchDao.updateScore(
            matchId   = entity.id,
            homeScore = dto.score.fullTime.home ?: 0,
            awayScore = dto.score.fullTime.away ?: 0,
            status    = newStatus,
            minute    = dto.minute,
        )
    }

    companion object {
        private const val TAG = "MatchRepository"
    }
}
