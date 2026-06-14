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
    private val teamDao: TeamDao,
    private val matchDao: MatchDao,
) {
    /**
     * Returns a reactive [Flow] of sorted standings for [groupId].
     * Recomputes automatically whenever a match result is written to the DB
     * (e.g. after a live score poll or a finished-match fetch).
     *
     * Sort order: points ↓ → goal difference ↓ → team name ↑
     */
    fun getGroupStandings(groupId: String): Flow<List<GroupStanding>> =
        combine(
            teamDao.getTeamsByGroup(groupId),
            matchDao.getCompletedMatchesForGroup(groupId),
        ) { teams, completedMatches ->
            computeStandings(teams, completedMatches)
        }

    // ── Computation ─────────────────────────────────────────────────────────

    private fun computeStandings(
        teams: List<TeamEntity>,
        matches: List<MatchWithTeams>,
    ): List<GroupStanding> {
        // Mutable record: teamId → [wins, draws, losses, goalsFor, goalsAgainst]
        val records = teams.associate { it.id to IntArray(5) }.toMutableMap()

        for (m in matches) {
            val hId = m.homeTeam.id
            val aId = m.awayTeam.id
            val hScore = m.match.homeScore
            val aScore = m.match.awayScore

            val h = records[hId] ?: continue
            val a = records[aId] ?: continue

            h[3] += hScore; h[4] += aScore   // home GF / GA
            a[3] += aScore; a[4] += hScore   // away GF / GA

            when {
                hScore > aScore -> { h[0]++; a[2]++ }  // home win
                hScore < aScore -> { a[0]++; h[2]++ }  // away win
                else            -> { h[1]++; a[1]++ }  // draw
            }
        }

        val teamById = teams.associateBy { it.id }

        return records.entries
            .mapNotNull { (id, r) ->
                val entity = teamById[id] ?: return@mapNotNull null
                val won = r[0]; val drawn = r[1]; val lost = r[2]
                val gf  = r[3]; val ga   = r[4]
                GroupStanding(
                    position       = 0,   // assigned after sorting
                    team           = Team(id = entity.id, name = entity.name, flag = entity.countryCode),
                    played         = won + drawn + lost,
                    won            = won,
                    drawn          = drawn,
                    lost           = lost,
                    goalsFor       = gf,
                    goalsAgainst   = ga,
                    goalDifference = gf - ga,
                    points         = won * 3 + drawn,
                )
            }
            .sortedWith(
                compareByDescending<GroupStanding> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenBy { it.team.name }
            )
            .mapIndexed { index, standing -> standing.copy(position = index + 1) }
    }
}
