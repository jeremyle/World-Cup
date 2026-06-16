package com.example.worldcup.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ScorersResponseDto(
    val scorers: List<ScorerDto> = emptyList(),
)

@Serializable
data class ScorerDto(
    val player: ScorerPlayerDto,
    val team: ScorerTeamDto = ScorerTeamDto(),
    val playedMatches: Int = 0,
    val goals: Int = 0,
    val assists: Int? = null,
    val penalties: Int? = null,
)

@Serializable
data class ScorerPlayerDto(
    val id: Int,
    val name: String,
    val dateOfBirth: String? = null,   // "1994-01-01"
    val nationality: String? = null,
    val position: String? = null,
)

@Serializable
data class ScorerTeamDto(
    val id: Int = 0,
    val name: String = "",
)
