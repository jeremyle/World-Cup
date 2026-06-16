package com.example.worldcup.data.repository

import android.util.Log
import com.example.worldcup.data.local.dao.PlayerStatDao
import com.example.worldcup.data.local.entity.PlayerStatEntity
import com.example.worldcup.data.local.entity.toDomain
import com.example.worldcup.data.model.PlayerStat
import com.example.worldcup.data.remote.FootballDataApi
import com.example.worldcup.data.remote.FootballDataApiClient
import com.example.worldcup.data.remote.dto.ScorerDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private const val TAG = "PlayerStatsRepository"
private const val MIN_REFRESH_INTERVAL_MS = 5 * 60 * 1000L

class PlayerStatsRepository(
    private val dao: PlayerStatDao,
    private val api: FootballDataApi = FootballDataApiClient.api,
) {

    @Volatile private var lastRefreshTime = 0L

    fun getTopScorers(): Flow<List<PlayerStat>> =
        dao.getTopScorers().map { list -> list.map { it.toDomain() } }

    fun getTopAssists(): Flow<List<PlayerStat>> =
        dao.getTopAssists().map { list -> list.map { it.toDomain() } }

    /**
     * Fetches the top 20 scorers from football-data.org, then derives:
     *  - Top scorers  → sorted by goals desc
     *  - Top assists  → sorted by assists desc
     *
     * Rate-limited to once per [MIN_REFRESH_INTERVAL_MS]; first call always fires.
     */
    suspend fun refresh() {
        val now = System.currentTimeMillis()
        if (lastRefreshTime != 0L && (now - lastRefreshTime) < MIN_REFRESH_INTERVAL_MS) {
            Log.d(TAG, "refresh: skipping — last fetch ${(now - lastRefreshTime) / 1000}s ago")
            return
        }

        Log.d(TAG, "refresh: fetching scorers from football-data.org")
        try {
            val scorers = api.getScorers(limit = 20).scorers

            val sorted = scorers.sortedByDescending { it.goals }
            var currentRank = 1
            val scorerEntities = sorted.take(10).mapIndexed { i, dto ->
                if (i > 0 && sorted[i].goals != sorted[i - 1].goals) currentRank++
                dto.toEntity(rank = currentRank, statType = "SCORER")
            }

            val assistEntities = scorers
                .filter { (it.assists ?: 0) > 0 }
                .sortedByDescending { it.assists ?: 0 }
                .take(10)
                .mapIndexed { i, dto -> dto.toEntity(rank = i + 1, statType = "ASSISTER") }

            dao.upsertAll(scorerEntities + assistEntities)
            lastRefreshTime = now
            Log.d(TAG, "refresh: upserted ${scorerEntities.size} scorers, ${assistEntities.size} assisters")
        } catch (e: Exception) {
            Log.e(TAG, "refresh: failed", e)
        }
    }
}

// ── Mapping ───────────────────────────────────────────────────────────────────

private fun ScorerDto.toEntity(rank: Int, statType: String) = PlayerStatEntity(
    id          = "${player.id}_$statType",
    rank        = rank,
    playerId    = player.id,
    playerName  = player.name,
    age         = player.dateOfBirth?.toAge() ?: 0,
    nationality = player.nationality ?: "",
    photoUrl    = "",   // football-data.org doesn't provide player photos
    teamName    = API_TO_DB_TEAM[team.name] ?: team.name,
    goals       = goals,
    assists     = assists ?: 0,
    statType    = statType,
)

/**
 * Maps football-data.org team names → exact DB team names (from worldcup_2026.json seed).
 * Only entries that differ between the two sources are listed here.
 */
private val API_TO_DB_TEAM = mapOf(
    "Korea Republic"          to "South Korea",
    "Bosnia and Herzegovina"  to "Bosnia & Herz.",
    "Bosnia-Herzegovina"      to "Bosnia & Herz.",
    "Congo DR"                to "DR Congo",
    "Ivory Coast"             to "Côte d'Ivoire",
    "Turkey"                  to "Türkiye",
    "Cape Verde Islands"      to "Cape Verde",
    "Curaçao"                 to "Curaçao",       // same, just ensuring it's present
)

private fun String.toAge(): Int = try {
    val dob   = LocalDate.parse(this)
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var age   = today.year - dob.year
    if (today.monthNumber < dob.monthNumber ||
        (today.monthNumber == dob.monthNumber && today.dayOfMonth < dob.dayOfMonth)) age--
    age
} catch (e: Exception) { 0 }
