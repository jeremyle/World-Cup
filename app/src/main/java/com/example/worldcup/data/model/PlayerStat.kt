package com.example.worldcup.data.model

data class PlayerStat(
    val rank: Int,
    val playerId: Int,
    val playerName: String,
    val age: Int,
    val nationality: String,
    val photoUrl: String,
    val teamName: String,
    val goals: Int,
    val assists: Int,
)
