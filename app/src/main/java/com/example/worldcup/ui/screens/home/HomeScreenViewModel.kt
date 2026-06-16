package com.example.worldcup.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldcup.data.local.database.WorldCupDatabase
import com.example.worldcup.data.local.entity.toDomain
import com.example.worldcup.data.model.GroupStanding
import com.example.worldcup.data.model.Match
import com.example.worldcup.data.repository.GroupRepository
import com.example.worldcup.data.repository.MatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

private const val TAG                  = "HomeScreenViewModel"
private const val LIVE_POLL_INTERVAL_MS = 60_000L

class HomeScreenViewModel(app: Application) : AndroidViewModel(app) {

    private val db              = WorldCupDatabase.getInstance(app)
    private val matchRepository = MatchRepository(db.matchDao())
    private val groupRepository = GroupRepository(db.matchDao(), db.teamDao())

    /** Reactive list of all matches ordered by kickoff time, driven by Room. */
    val allMatches: StateFlow<List<Match>> =
        db.matchDao().getAllMatchesWithTeams()
            .map { list -> list.map { it.toDomain() } }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    /** Reactive list of today's matches, driven by Room. */
    val todaysMatches: StateFlow<List<Match>> = run {
        val zone    = TimeZone.currentSystemDefault()
        val today   = Clock.System.todayIn(zone)
        val startMs = today.atStartOfDayIn(zone).toEpochMilliseconds()
        val endMs   = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()

        db.matchDao().getMatchesForDay(startMs, endMs)
            .map { list -> list.map { it.toDomain() } }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
    }

    /** Reactive standings for any group — computed locally from match results. */
    fun getGroupStandings(groupId: String): Flow<List<GroupStanding>> =
        groupRepository.getGroupStandings(groupId)

    /**
     * The 8 best third-place team IDs that qualify for the knockout phase,
     * ranked by official FIFA WC 2026 third-place criteria.
     * Updates reactively whenever any group's results change.
     */
    val qualifyingThirdPlaceIds: StateFlow<Set<String>> =
        groupRepository.getQualifyingThirdPlaceTeamIds()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

    // ── Per-group live polling ───────────────────────────────────────────────

    private var groupPollingJob: Job? = null

    /**
     * Called by the UI via LaunchedEffect whenever the visible group changes.
     *
     * Starts a poll loop that:
     *  1. Immediately refreshes live match scores if the group has any live matches.
     *  2. Continues polling every 60 s while live matches remain.
     *  3. Stops automatically when no live matches remain.
     *
     * Standings are recomputed automatically by [GroupRepository] whenever the
     * DB changes — no explicit standings refresh is needed here.
     */
    fun onGroupSelected(groupId: String?) {
        groupPollingJob?.cancel()
        groupPollingJob = null
        if (groupId == null) return

        groupPollingJob = viewModelScope.launch {
            // Immediate refresh if this group already has live matches in DB
            val initialLive = db.matchDao().countLiveMatchesInGroup(groupId)
            if (initialLive > 0) {
                runCatching { matchRepository.refreshLiveMatches() }
                    .onFailure { Log.e(TAG, "refreshLiveMatches failed (initial)", it) }
            }

            // Poll while the group has live matches
            while (isActive) {
                delay(LIVE_POLL_INTERVAL_MS)
                val liveCount = db.matchDao().countLiveMatchesInGroup(groupId)
                if (liveCount == 0) {
                    Log.d(TAG, "No live matches in group $groupId — stopping poll")
                    break
                }
                runCatching { matchRepository.refreshLiveMatches() }
                    .onFailure { Log.e(TAG, "refreshLiveMatches failed (poll)", it) }
            }
        }
    }

    // ── Init ────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            // 1. Bring all past finished match scores up to date (permanent cache —
            //    skips API if every past match is already COMPLETED in DB).
            runCatching { matchRepository.refreshAllFinishedMatches() }
                .onFailure { Log.e(TAG, "refreshAllFinishedMatches failed", it) }

            // 2. Pick up any matches that went live since the last session.
            runCatching { matchRepository.refreshLiveMatches() }
                .onFailure { Log.e(TAG, "refreshLiveMatches failed (init)", it) }
        }
    }
}
