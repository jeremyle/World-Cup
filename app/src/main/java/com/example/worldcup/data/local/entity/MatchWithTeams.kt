package com.example.worldcup.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.example.worldcup.data.model.Location
import com.example.worldcup.data.model.Match
import com.example.worldcup.data.model.Stadium
import com.example.worldcup.data.model.Status
import com.example.worldcup.data.model.Team
import kotlinx.datetime.Instant

/**
 * Room relation that joins a match with its home team, away team, and stadium
 * in a single query. Use this instead of MatchEntity when you need display data.
 */
data class MatchWithTeams(
    @Embedded val match: MatchEntity,

    @Relation(
        parentColumn = "homeTeamId",
        entityColumn = "id"
    )
    val homeTeam: TeamEntity,

    @Relation(
        parentColumn = "awayTeamId",
        entityColumn = "id"
    )
    val awayTeam: TeamEntity,

    @Relation(
        parentColumn = "stadiumId",
        entityColumn = "id"
    )
    val stadium: StadiumEntity,
)

fun MatchWithTeams.toDomain(): Match = Match(
    id = match.id,
    homeTeam = Team(id = homeTeam.id, name = homeTeam.name, flag = homeTeam.countryCode),
    awayTeam = Team(id = awayTeam.id, name = awayTeam.name, flag = awayTeam.countryCode),
    stadium = Stadium(
        id = stadium.id,
        name = stadium.name,
        location = Location(city = stadium.city, state = "", country = stadium.country),
    ),
    kickoffTime = Instant.fromEpochMilliseconds(match.kickoffTimeMs),
    status = when (match.status) {
        "LIVE"      -> Status.LIVE
        "COMPLETED" -> Status.COMPLETED
        else        -> Status.UPCOMING
    },
    homeTeamScore = match.homeScore,
    awayTeamScore = match.awayScore,
    minute = match.minute,
)
