package com.example.worldcup.data.repository

import com.example.worldcup.data.local.dao.MatchDao
import com.example.worldcup.data.local.dao.TeamDao
import com.example.worldcup.data.local.entity.MatchWithTeams
import com.example.worldcup.data.local.entity.TeamEntity
import com.example.worldcup.data.model.GroupStanding
import com.example.worldcup.data.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GroupRepository(
    private val matchDao: MatchDao,
    private val teamDao: TeamDao,
) {
    /**
     * Returns a reactive [Flow] of standings for [groupId], computed locally from
     * match results stored in the DB.
     *
     * Teams are ordered according to the official FIFA World Cup 2026 regulations
     * (Article 32, page 26). See [rank] for the full tiebreaker sequence.
     *
     * The flow re-emits automatically whenever any match score or status changes,
     * so live standings update in real time without an extra API call.
     */
    fun getGroupStandings(groupId: String): Flow<List<GroupStanding>> =
        combine(
            matchDao.getPlayedMatchesForGroup(groupId),
            teamDao.getTeamsByGroup(groupId),
        ) { matches, teams ->
            val stats    = buildStats(matches, teams)
            val liveTeams = matches
                .filter { it.match.status == "LIVE" }
                .flatMap { listOf(it.match.homeTeamId, it.match.awayTeamId) }
                .toSet()

            rank(stats.values.toList(), matches)
                .mapIndexed { i, s ->
                    GroupStanding(
                        position       = i + 1,
                        team           = Team(id = s.team.id, name = s.team.name, flag = s.team.countryCode),
                        played         = s.played,
                        won            = s.won,
                        drawn          = s.drawn,
                        lost           = s.lost,
                        goalsFor       = s.goalsFor,
                        goalsAgainst   = s.goalsAgainst,
                        goalDifference = s.goalDifference,
                        points         = s.points,
                        isLive         = s.team.id in liveTeams,
                    )
                }
        }

    /**
     * Returns a reactive [Flow] of the **8 qualifying third-place team IDs** across all
     * 12 groups, ranked by the official FIFA WC 2026 criteria for third-place teams:
     *
     *  1. Points
     *  2. Goal difference in all group matches
     *  3. Goals scored in all group matches
     *  4. Team name A–Z  (proxy for fair-play / FIFA ranking, unavailable from free API)
     *
     * The flow re-emits whenever any group's standings change (e.g. a match finishes or
     * goes live), so the yellow qualifying highlight updates in real time.
     */
    fun getQualifyingThirdPlaceTeamIds(): Flow<Set<String>> {
        val groupIds = ('A'..'L').map { it.toString() }
        val flows    = groupIds.map { getGroupStandings(it) }
        // combine(List<Flow>) → Flow<Array<T>>
        return combine(flows) { allStandings ->
            allStandings
                .mapNotNull { standings -> standings.getOrNull(2) } // 3rd-place team per group
                .sortedWith(
                    compareByDescending<GroupStanding> { it.points }
                        .thenByDescending { it.goalDifference }
                        .thenByDescending { it.goalsFor }
                        .thenBy { it.team.name }
                )
                .take(8)
                .map { it.team.id }
                .toSet()
        }
    }

    // ── Stats accumulation ───────────────────────────────────────────────────

    private fun buildStats(
        matches: List<MatchWithTeams>,
        teams: List<TeamEntity>,
    ): LinkedHashMap<String, TeamStats> {
        val stats = LinkedHashMap<String, TeamStats>()
        teams.forEach { stats[it.id] = TeamStats(it) }

        for (mwt in matches) {
            val h = stats[mwt.match.homeTeamId] ?: continue
            val a = stats[mwt.match.awayTeamId] ?: continue
            val hg = mwt.match.homeScore
            val ag = mwt.match.awayScore

            h.played++; a.played++
            h.goalsFor += hg; h.goalsAgainst += ag
            a.goalsFor += ag; a.goalsAgainst += hg

            when {
                hg > ag -> { h.won++;  a.lost++  }
                hg < ag -> { a.won++;  h.lost++  }
                else    -> { h.drawn++; a.drawn++ }
            }
        }

        return stats
    }

    // ── FIFA 2026 ranking (Article 32) ───────────────────────────────────────

    /**
     * Ranks a list of teams applying the official FIFA 2026 tiebreaker sequence:
     *
     *  1. Points (primary sort — callers pre-group by points before calling [resolveTie])
     *  2. Goal difference in all group matches
     *  3. Goals scored in all group matches
     *  4. Points in H2H matches among the tied teams
     *  5. Goal difference in H2H matches among the tied teams
     *  6. Goals scored in H2H matches among the tied teams
     *  7. If a smaller subset is still tied, criteria 4–6 are re-applied to just those teams
     *  8. Team name A–Z  (proxy for fair-play score and FIFA ranking, which require data
     *                     not available from the free-tier API)
     */
    private fun rank(
        teams: List<TeamStats>,
        allMatches: List<MatchWithTeams>,
    ): List<TeamStats> {
        // Sort by points, then delegate tied groups to resolveTie()
        val byPoints = teams.sortedByDescending { it.points }
        val result   = mutableListOf<TeamStats>()
        var i = 0
        while (i < byPoints.size) {
            val pts      = byPoints[i].points
            val tiedGroup = byPoints.subList(i, byPoints.size).takeWhile { it.points == pts }
            result += if (tiedGroup.size == 1) tiedGroup else resolveTie(tiedGroup, allMatches)
            i += tiedGroup.size
        }
        return result
    }

    /**
     * Resolves a group of teams equal on points.
     * Applies criteria 2–3 (overall GD/GF), then hands still-tied subgroups
     * off to [resolveByH2H].
     */
    private fun resolveTie(
        tied: List<TeamStats>,
        allMatches: List<MatchWithTeams>,
    ): List<TeamStats> {
        // Criteria 2 & 3 — overall GD then GF
        val byOverall = tied.sortedWith(
            compareByDescending<TeamStats> { it.goalDifference }
                .thenByDescending { it.goalsFor }
        )

        val result = mutableListOf<TeamStats>()
        var i = 0
        while (i < byOverall.size) {
            val ref      = byOverall[i]
            val subgroup = byOverall.subList(i, byOverall.size)
                .takeWhile { it.goalDifference == ref.goalDifference && it.goalsFor == ref.goalsFor }

            result += if (subgroup.size == 1) subgroup else resolveByH2H(subgroup, allMatches)
            i += subgroup.size
        }
        return result
    }

    /**
     * Criteria 4–7: head-to-head points, GD, GF among [tied] teams.
     *
     * If a smaller subset remains tied after H2H, the function recurses on just
     * that subset (re-applying H2H exclusively to matches between those teams).
     * If the entire group is still tied (H2H offered no separation), falls through
     * to team name A–Z as a deterministic final tiebreaker.
     */
    private fun resolveByH2H(
        tied: List<TeamStats>,
        allMatches: List<MatchWithTeams>,
    ): List<TeamStats> {
        if (tied.size == 1) return tied

        // Compute H2H stats using only matches between the tied teams
        val ids = tied.map { it.team.id }.toSet()

        data class H2HStats(var pts: Int = 0, var gd: Int = 0, var gf: Int = 0)
        val h2h = tied.associate { it.team.id to H2HStats() }

        for (mwt in allMatches) {
            val hId = mwt.match.homeTeamId
            val aId = mwt.match.awayTeamId
            if (hId !in ids || aId !in ids) continue

            val hh = h2h[hId]!!; val ah = h2h[aId]!!
            val hg = mwt.match.homeScore; val ag = mwt.match.awayScore

            hh.gf += hg; hh.gd += hg - ag
            ah.gf += ag; ah.gd += ag - hg
            when {
                hg > ag -> hh.pts += 3
                hg < ag -> ah.pts += 3
                else    -> { hh.pts += 1; ah.pts += 1 }
            }
        }

        // Sort by H2H criteria 4–6
        val byH2H = tied.sortedWith(
            compareByDescending<TeamStats> { h2h[it.team.id]!!.pts }
                .thenByDescending { h2h[it.team.id]!!.gd }
                .thenByDescending { h2h[it.team.id]!!.gf }
        )

        val result = mutableListOf<TeamStats>()
        var i = 0
        while (i < byH2H.size) {
            val ref      = h2h[byH2H[i].team.id]!!
            val subgroup = byH2H.subList(i, byH2H.size)
                .takeWhile { h2h[it.team.id]!!.let { s -> s.pts == ref.pts && s.gd == ref.gd && s.gf == ref.gf } }

            result += when {
                subgroup.size == 1         -> subgroup
                subgroup.size < tied.size  -> resolveByH2H(subgroup, allMatches) // criteria 7: re-apply H2H to smaller subset
                else                       -> subgroup.sortedBy { it.team.name }  // H2H fully exhausted → name A–Z
            }
            i += subgroup.size
        }
        return result
    }

    // ── Domain model ─────────────────────────────────────────────────────────

    private class TeamStats(val team: TeamEntity) {
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
