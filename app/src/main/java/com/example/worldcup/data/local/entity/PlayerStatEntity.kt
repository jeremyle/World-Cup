package com.example.worldcup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.worldcup.data.model.PlayerStat

@Entity(tableName = "player_stats")
data class PlayerStatEntity(
    /** Composite key: "${playerId}_${statType}" — stable across re-fetches. */
    @PrimaryKey val id: String,
    val rank: Int,
    val playerId: Int,
    val playerName: String,
    val age: Int,
    val nationality: String,
    val photoUrl: String,
    val teamName: String,
    val goals: Int,
    val assists: Int,
    /** "SCORER" or "ASSISTER" — discriminates the two leaderboards. */
    val statType: String,
)

fun PlayerStatEntity.toDomain() = PlayerStat(
    rank        = rank,
    playerId    = playerId,
    playerName  = playerName,
    age         = age,
    nationality = nationality,
    photoUrl    = photoUrl,
    teamName    = teamName,
    goals       = goals,
    assists     = assists,
)
