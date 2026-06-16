package com.example.worldcup.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldcup.data.local.database.WorldCupDatabase
import com.example.worldcup.data.local.entity.toDomain
import com.example.worldcup.data.model.GroupStanding
import com.example.worldcup.data.model.Match
import com.example.worldcup.data.model.PlayerStat
import com.example.worldcup.data.repository.GroupRepository
import com.example.worldcup.data.repository.MatchRepository
import com.example.worldcup.data.repository.PlayerStatsRepository
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

private const val TAG                   = "HomeScreenViewModel"
private const val LIVE_POLL_INTERVAL_MS = 60_000L

class HomeScreenViewModel(app: Application) : AndroidViewModel(app) {

    private val db                   = WorldCupDatabase.getInstance(app)
    private val matchRepository      = MatchRepository(db.matchDao())
    private val groupRepository      = GroupRepository(db.matchDao(), db.teamDao())
    private val playerStatsRepository = PlayerStatsRepository(db.playerStatDao())

    // ── Match flows ─────────────────────────────────────────────────────────

    /** All matches ordered by kickoff time. */
    val allMatches: StateFlow<List<Match>> =
        db.matchDao().getAllMatchesWithTeams()
            .map { list -> list.map { it.toDomain() } }
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    /** Today's matches. */
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

    // ── Group standings ──────────────────────────────────────────────────────

    fun getGroupStandings(groupId: String): Flow<List<GroupStanding>> =
        groupRepository.getGroupStandings(groupId)

    val qualifyingThirdPlaceIds: StateFlow<Set<String>> =
        groupRepository.getQualifyingThirdPlaceTeamIds()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

    // ── Player stats ─────────────────────────────────────────────────────────

    val topScorers: StateFlow<List<PlayerStat>> =
        playerStatsRepository.getTopScorers()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val topAssists: StateFlow<List<PlayerStat>> =
        playerStatsRepository.getTopAssists()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    // ── Per-group live polling ───────────────────────────────────────────────

    private var groupPollingJob: Job? = null

    fun onGroupSelected(groupId: String?) {
        groupPollingJob?.cancel()
        groupPollingJob = null
        if (groupId == null) return

        groupPollingJob = viewModelScope.launch {
            // Immediate refresh if this group already has live matches in DB
            val initialLive = db.matchDao().countLiveMatchesInGroup(groupId)
            if (initialLive > 0) {
                val changed = runCatching { matchRepository.refreshLiveMatches() }.getOrDefault(false)
                if (changed) refreshPlayerStats()
            }

            while (isActive) {
                delay(LIVE_POLL_INTERVAL_MS)
                val liveCount = db.matchDao().countLiveMatchesInGroup(groupId)
                if (liveCount == 0) {
                    Log.d(TAG, "No live matches in group $groupId — stopping poll")
                    break
                }
                val changed = runCatching { matchRepository.refreshLiveMatches() }.getOrDefault(false)
                if (changed) refreshPlayerStats()
            }
        }
    }

    // ── Init ────────────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            // 1. Sync finished match scores
            val finishedChanged = runCatching { matchRepository.refreshAllFinishedMatches() }
                .onFailure { Log.e(TAG, "refreshAllFinishedMatches failed", it) }
                .getOrDefault(false)

            // 2. Sync live matches
            val liveChanged = runCatching { matchRepository.refreshLiveMatches() }
                .onFailure { Log.e(TAG, "refreshLiveMatches failed (init)", it) }
                .getOrDefault(false)

            // 3. Player stats — always fetch on app start (covers cold launch)
            //    and again if match data changed
            refreshPlayerStats()
        }
    }

    private suspend fun refreshPlayerStats() {
        runCatching { playerStatsRepository.refresh() }
            .onFailure { Log.e(TAG, "refreshPlayerStats failed", it) }
    }
}
