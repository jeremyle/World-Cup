package com.example.worldcup.ui.screens.fixtures

import com.example.worldcup.ui.util.PredictiveBackContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.worldcup.data.model.Match
import com.example.worldcup.ui.screens.home.HomeScreenViewModel
import com.example.worldcup.ui.screens.home.MatchCard
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Flat list item — either a date header or a match card
private sealed interface FixtureItem {
    data class Header(val date: LocalDate) : FixtureItem
    data class MatchRow(val match: Match) : FixtureItem
}

private fun buildItems(matches: List<Match>): List<FixtureItem> {
    val tz = TimeZone.currentSystemDefault()
    val items = mutableListOf<FixtureItem>()
    var lastDate: LocalDate? = null
    for (match in matches) {
        val date = match.kickoffTime.toLocalDateTime(tz).date
        if (date != lastDate) {
            items += FixtureItem.Header(date)
            lastDate = date
        }
        items += FixtureItem.MatchRow(match)
    }
    return items
}

private fun LocalDate.formatted(): String {
    val month = monthNumber.toString().padStart(2, '0')
    val day   = dayOfMonth.toString().padStart(2, '0')
    return "${dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, $month/$day/$year"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixturesScreen(
    currentMatchId: String?,
    viewModel: HomeScreenViewModel,
    onBack: () -> Unit,
) {
    val allMatches by viewModel.allMatches.collectAsState()
    val listState = rememberLazyListState()

    // Build flat item list; recompute only when matches change
    val items = remember(allMatches) { buildItems(allMatches) }

    // Scroll to current match on first render
    LaunchedEffect(items, currentMatchId) {
        if (currentMatchId == null) return@LaunchedEffect
        val index = items.indexOfFirst { it is FixtureItem.MatchRow && it.match.id == currentMatchId }
        if (index >= 0) listState.scrollToItem(index)
    }

    PredictiveBackContainer(onBack = onBack) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Fixtures",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            items(
                count = items.size,
                key   = { index ->
                    when (val item = items[index]) {
                        is FixtureItem.Header   -> "header_${item.date}"
                        is FixtureItem.MatchRow -> item.match.id
                    }
                },
            ) { index ->
                when (val item = items[index]) {
                    is FixtureItem.Header -> {
                        Column {
                            Text(
                                text = item.date.formatted(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    is FixtureItem.MatchRow -> {
                        MatchCard(
                            match = item.match,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    } // PredictiveBackContainer
}
