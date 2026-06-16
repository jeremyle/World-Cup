package com.example.worldcup.data.remote

import com.example.worldcup.data.remote.dto.ApiFootballResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiFootballService {

    @GET("players/topscorers")
    suspend fun getTopScorers(
        @Query("league") league: Int = 1,
        @Query("season") season: Int = 2026,
    ): ApiFootballResponse

    @GET("players/topassists")
    suspend fun getTopAssists(
        @Query("league") league: Int = 1,
        @Query("season") season: Int = 2026,
    ): ApiFootballResponse
}
