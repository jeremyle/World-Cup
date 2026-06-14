package com.example.worldcup.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatchesResponse(
    val matches: List<MatchDto> = emptyList(),
)

@Serializable
data class MatchDto(
    val utcDate: String,       // "2026-06-13T22:00:00Z"
    val status: String,        // SCHEDULED, TIMED, IN_PLAY, PAUSED, FINISHED, etc.
    val minute: Int? = null,   // null when not in play
    val homeTeam: TeamDto,
    val awayTeam: TeamDto,
    val score: ScoreDto,
)

@Serializable
data class TeamDto(
    val tla: String? = null,   // FIFA 3-letter code: "BRA", "ARG" — may be null pre-tournament
    val name: String = "",
)

@Serializable
data class ScoreDto(
    val fullTime: GoalsDto = GoalsDto(),
)

@Serializable
data class GoalsDto(
    val home: Int? = null,   // null before the match
    val away: Int? = null,
)
