package com.example.worldcup.data.local.database

import android.content.Context
import com.example.worldcup.data.local.entity.MatchEntity
import com.example.worldcup.data.local.entity.StadiumEntity
import com.example.worldcup.data.local.entity.TeamEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.datetime.Instant

/**
 * Seeds the Room database from worldcup_2026.json in assets.
 * Call [seedIfEmpty] on app startup — it is a no-op if data already exists.
 */
object DatabaseSeeder {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfEmpty(context: Context, db: WorldCupDatabase) {
        withContext(Dispatchers.IO) {
            // Only seed once
            if (db.teamDao().count() > 0) return@withContext

            val raw = context.assets.open("worldcup_2026.json")
                .bufferedReader()
                .use { it.readText() }

            val seed = json.decodeFromString<SeedData>(raw)

            db.stadiumDao().insertAll(seed.stadiums.map { it.toEntity() })
            db.teamDao().insertAll(seed.teams.map { it.toEntity() })
            db.matchDao().insertAll(seed.matches.map { it.toEntity() })
        }
    }

    // ── JSON DTOs ─────────────────────────────────────────────────────────────

    @Serializable
    private data class SeedData(
        val stadiums: List<StadiumDto>,
        val teams: List<TeamDto>,
        val matches: List<MatchDto>,
    )

    @Serializable
    private data class StadiumDto(
        val id: String,
        val name: String,
        val city: String,
        val country: String,
        val capacity: Int,
    ) {
        fun toEntity() = StadiumEntity(
            id = id,
            name = name,
            city = city,
            country = country,
            capacity = capacity,
        )
    }

    @Serializable
    private data class TeamDto(
        val id: String,
        val name: String,
        val countryCode: String,
        val groupId: String,
    ) {
        fun toEntity() = TeamEntity(
            id = id,
            name = name,
            countryCode = countryCode,
            groupId = groupId,
        )
    }

    @Serializable
    private data class MatchDto(
        val id: String,
        val phase: String,
        val groupId: String? = null,
        val matchday: Int? = null,
        val homeTeamId: String,
        val awayTeamId: String,
        val stadiumId: String,
        val kickoffTime: String,  // ISO-8601 UTC, e.g. "2026-06-11T19:00:00Z"
    ) {
        fun toEntity() = MatchEntity(
            id = id,
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId,
            stadiumId = stadiumId,
            kickoffTimeMs = Instant.parse(kickoffTime).toEpochMilliseconds(),
            phase = phase,
            groupId = groupId,
            matchday = matchday,
        )
    }
}
