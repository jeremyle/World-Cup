package com.example.worldcup.data

import com.example.worldcup.data.model.Location
import com.example.worldcup.data.model.Match
import com.example.worldcup.data.model.Stadium
import com.example.worldcup.data.model.Status
import com.example.worldcup.data.model.Team
import kotlinx.datetime.Instant

object StubData {

    private val metLifeStadium = Stadium(
        id = "s1",
        name = "MetLife Stadium",
        location = Location("East Rutherford", "New Jersey", "USA")
    )

    private val sofiStadium = Stadium(
        id = "s2",
        name = "SoFi Stadium",
        location = Location("Inglewood", "California", "USA")
    )

    private val aztecaStadium = Stadium(
        id = "s3",
        name = "Estadio Azteca",
        location = Location("Mexico City", "CDMX", "Mexico")
    )

    val matches = listOf(
        Match(
            id = "m1",
            homeTeam = Team("t1", "Brazil", "BR"),
            awayTeam = Team("t2", "Argentina", "AR"),
            stadium = metLifeStadium,
            kickoffTime = Instant.parse("2026-06-15T18:00:00Z"),
            status = Status.LIVE,
            homeTeamScore = 1,
            awayTeamScore = 0,
            minute = 67
        ),
        Match(
            id = "m2",
            homeTeam = Team("t3", "France", "FR"),
            awayTeam = Team("t4", "Germany", "DE"),
            stadium = sofiStadium,
            kickoffTime = Instant.parse("2026-06-15T21:00:00Z"),
            status = Status.LIVE,
            homeTeamScore = 2,
            awayTeamScore = 2,
            minute = 45
        ),
        Match(
            id = "m3",
            homeTeam = Team("t5", "Spain", "ES"),
            awayTeam = Team("t6", "Portugal", "PT"),
            stadium = aztecaStadium,
            kickoffTime = Instant.parse("2026-06-15T23:00:00Z"),
            status = Status.COMPLETED,
            homeTeamScore = 0,
            awayTeamScore = 1,
        ),
        Match(
            id = "m4",
            homeTeam = Team("t7", "USA", "US"),
            awayTeam = Team("t8", "Mexico", "MX"),
            stadium = metLifeStadium,
            kickoffTime = Instant.parse("2026-06-16T20:00:00Z"),
            status = Status.UPCOMING,
            homeTeamScore = 0,
            awayTeamScore = 0,
        ),
    )
}
