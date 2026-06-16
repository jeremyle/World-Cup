package com.example.worldcup.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiFootballResponse(
    val response: List<PlayerEntryDto> = emptyList(),
)

@Serializable
data class PlayerEntryDto(
    val player: PlayerInfoDto,
    val statistics: List<PlayerStatisticsDto> = emptyList(),
)

@Serializable
data class PlayerInfoDto(
    val id: Int,
    val name: String,
    val age: Int = 0,
    val nationality: String = "",
    val photo: String = "",
)

@Serializable
data class PlayerStatisticsDto(
    val team: TeamRefDto = TeamRefDto(),
    val goals: GoalStatsDto = GoalStatsDto(),
)

@Serializable
data class TeamRefDto(
    val name: String = "",
)

@Serializable
data class GoalStatsDto(
    val total: Int? = null,
    val assists: Int? = null,
)
