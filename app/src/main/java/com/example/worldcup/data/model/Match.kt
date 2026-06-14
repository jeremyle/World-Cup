package com.example.worldcup.data.model

import kotlinx.datetime.Instant

data class Match(
    val id: String,
    val homeTeam: Team,
    val awayTeam: Team,
    val stadium: Stadium,
    val kickoffTime: Instant,
    val status: Status,
    val homeTeamScore: Int,
    val awayTeamScore: Int,
    val minute: Int? = null,  // non-null only when status == LIVE
)

enum class Status {
    UPCOMING, LIVE, COMPLETED
}