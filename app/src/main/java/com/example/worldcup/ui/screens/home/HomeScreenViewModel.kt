package com.example.worldcup.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldcup.data.local.database.WorldCupDatabase
import com.example.worldcup.data.local.entity.toDomain
import com.example.worldcup.data.model.Match
import com.example.worldcup.data.repository.MatchRepository
import kotlinx.coroutines.delay
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

private const val TAG = "HomeScreenViewModel"
private const val LIVE_POLL_INTERVAL_MS = 60_000L

class HomeScreenViewModel(app: Application) : AndroidViewModel(app) {

    private val db         = WorldCupDatabase.getInstance(app)
    private val repository = MatchRepository(db.matchDao())

    val todaysMatches: StateFlow<List<Match>> = run {
        val zone    = TimeZone.currentSystemDefault()
        val today   = Clock.System.todayIn(zone)
        val startMs = today.atStartOfDayIn(zone).toEpochMilliseconds()
        val endMs   = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()

        db.matchDao().getMatchesForDay(startMs, endMs)
            .map { list -> list.map { it.toDomain() } }
            .stateIn(
                scope          = viewModelScope,
                started        = SharingStarted.WhileSubscribed(5_000),
                initialValue   = emptyList(),
            )
    }

    init {
        val zone  = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)

        viewModelScope.launch {
            // Step 1 — fetch finished matches once; DB acts as permanent cache
            runCatching { repository.refreshFinishedMatches(today) }
                .onFailure { Log.e(TAG, "refreshFinishedMatches failed", it) }

            // Step 2 — poll live matches every 60 s
            while (isActive) {
                runCatching { repository.refreshLiveMatches() }
                    .onFailure { Log.e(TAG, "refreshLiveMatches failed", it) }
                delay(LIVE_POLL_INTERVAL_MS)
            }
        }
    }
}
