package com.example.worldcup.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.worldcup.data.local.database.WorldCupDatabase
import com.example.worldcup.data.local.entity.toDomain
import com.example.worldcup.data.model.Match
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class HomeScreenViewModel(app: Application) : AndroidViewModel(app) {

    private val matchDao = WorldCupDatabase.getInstance(app).matchDao()

    val todaysMatches: StateFlow<List<Match>> = run {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        val startMs = today.atStartOfDayIn(zone).toEpochMilliseconds()
        val endMs   = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()

        matchDao.getMatchesForDay(startMs, endMs)
            .map { list -> list.map { it.toDomain() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
    }
}
