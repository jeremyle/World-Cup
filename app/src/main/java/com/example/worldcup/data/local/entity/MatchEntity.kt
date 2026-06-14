package com.example.worldcup.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["homeTeamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TeamEntity::class,
            parentColumns = ["id"],
            childColumns = ["awayTeamId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StadiumEntity::class,
            parentColumns = ["id"],
            childColumns = ["stadiumId"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index("homeTeamId"),
        Index("awayTeamId"),
        Index("stadiumId"),
        Index("kickoffTimeMs"),
    ]
)
data class MatchEntity(
    @PrimaryKey val id: String,
    val homeTeamId: String,
    val awayTeamId: String,
    val stadiumId: String,
    val kickoffTimeMs: Long,        // UTC epoch milliseconds
    val phase: String,              // GROUP, R32, QF, SF, THIRD_PLACE, FINAL
    val groupId: String?,           // null for knockout rounds
    val matchday: Int?,             // 1, 2, or 3 for group stage; null for knockouts
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val status: String = "UPCOMING",// UPCOMING, LIVE, COMPLETED
    val minute: Int? = null,
)
