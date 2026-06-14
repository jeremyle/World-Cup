package com.example.worldcup.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stadiums")
data class StadiumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val city: String,
    val country: String,
    val capacity: Int,
)
