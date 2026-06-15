package com.example.worldcup.data.repository

import com.example.worldcup.data.local.dao.MatchDao
import com.example.worldcup.data.local.dao.TeamDao
import com.example.worldcup.data.local.entity.MatchWithTeams
import com.example.worldcup.data.local.entity.TeamEntity
import com.example.worldcup.data.model.GroupStanding
import com.example.worldcup.data.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GroupRepository(
    private val matchDao: MatchDao,
    private val teamDao: TeamDao,
) {
    /**
     * Returns a reactive [Flow] of standings for [groupId], computed locally from
     * match results stored in the DB.
     *
     * Standing points are calculated from COMPLETED and LIVE matches:
     *   Win  → 3 pts, Loss → 0 pts, Draw → 1 pt each
     * Teams with no played matches appear at the bottom with zeroed stats.
     *
     * The flow re-emits automatically whenever any match score or status changes,
     * so live standings update in real time without an extra API call.
     *
     * Sort order: Pts desc → GD desc → GF desc → Name asc (FIFA tiebreakers, simplified).
     */
    fun getGroupStandings(groupId: String): Flow<List<GroupStanding>> =
        combine(
            matchDao.getPlayedMatchesForGroup(groupId),
            teamDao.getTeamsByGroup(groupId),
        ) { matches, teams ->
            computeStandings(matches, teams)
        }

    // ── Computation ─────────────────────────────────────────────────────────

    private fun computeStandings(
        matches: List<MatchWithTeams>,
        teams: List<TeamEntity>,
    ): List<GroupStanding> {
        // Initialise a stats entry for every team in the group (even those with 0 matches)
        val stats = LinkedHashMap<String, Stats>()
        teams.forEach { stats[it.id] = Stats(it) }

        for (mwt in matches) {
            val homeId = mwt.match.homeTeamId
            val awayId = mwt.match.awayTeamId
            val h      = mwt.match.homeScore
            val a      = mwt.match.awayScore

            val homeStats = stats[homeId] ?: continue
            val awayStats = stats[awayId] ?: continue

            homeStats.played++
            awayStats.played++
            homeStats.goalsFor      += h
            homeStats.goalsAgainst  += a
            awayStats.goalsFor      += a
            awayStats.goalsAgainst  += h

            when {
                h > a -> { homeStats.won++;  awayStats.lost++ }
                h < a -> { awayStats.won++;  homeStats.lost++ }
                else  -> { homeStats.drawn++; awayStats.drawn++ }
            }
        }

        return stats.values
            .sortedWith(
                compareByDescending<Stats> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor }
                    .thenBy { it.team.name }
            )
            .mapIndexed { index, s ->
                GroupStanding(
                    position       = index + 1,
                    team           = Team(id = s.team.id, name = s.team.name, flag = s.team.countryCode),
                    played         = s.played,
                    won            = s.won,
                    drawn          = s.drawn,
                    lost           = s.lost,
                    goalsFor       = s.goalsFor,
                    goalsAgainst   = s.goalsAgainst,
                    goalDifference = s.goalDifference,
                    points         = s.points,
                )
            }
    }

    private class Stats(val team: TeamEntity) {
        var played       = 0
        var won          = 0
        var drawn        = 0
        var lost         = 0
        var goalsFor     = 0
        var goalsAgainst = 0

        val goalDifference get() = goalsFor - goalsAgainst
        val points         get() = won * 3 + drawn
    }
}
