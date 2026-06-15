package com.example.worldcup.data.remote

import com.example.worldcup.data.remote.dto.MatchesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface FootballDataApi {

    /**
     * Fetch World Cup matches with optional filters.
     *
     * Examples:
     *   status=FINISHED, dateFrom=2026-06-11, dateTo=2026-06-14  → all finished matches in range
     *   status=LIVE                                               → all currently live matches
     */
    @GET("competitions/WC/matches")
    suspend fun getMatches(
        @Query("status")   status:   String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo")   dateTo:   String? = null,
    ): MatchesResponse
}
