package com.example.worldcup.data.model

data class Stadium(
    val id: String,
    val name: String,
    val location: Location,
)

data class Location(
    val city: String,
    val state: String,
    val country: String,
)
