package com.example.worldcup.data.model

data class Record(
    val team: Team,
    val gamePlayed: Int = 0,
    val points: Int = 0,
    val goalDifference: Int = 0,
    val goalScored: Int = 0,
    val goalConceded: Int = 0,
)
