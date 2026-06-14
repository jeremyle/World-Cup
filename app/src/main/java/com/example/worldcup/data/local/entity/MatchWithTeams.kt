package com.example.worldcup.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

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
