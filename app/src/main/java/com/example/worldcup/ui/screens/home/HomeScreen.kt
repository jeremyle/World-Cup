package com.example.worldcup.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worldcup.data.model.GroupStanding
import com.example.worldcup.ui.components.CalendarIconButton
import com.example.worldcup.ui.components.TopBar
import com.example.worldcup.ui.screens.groups.GroupCard
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel = viewModel()) {
    val matches by viewModel.todaysMatches.collectAsState()
    val pagerState = rememberPagerState(pageCount = { matches.size })
    val selectedDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    // Derive the group of whichever match card is currently visible
    val currentGroupId = matches.getOrNull(pagerState.currentPage)?.groupId

    // Notify the ViewModel when the visible group changes so it can start/stop polling
    LaunchedEffect(currentGroupId) {
        viewModel.onGroupSelected(currentGroupId)
    }

    // Reactively collect standings for the current group; switches automatically on swipe
    val groupStandings by produceState<List<GroupStanding>>(
        initialValue = emptyList(),
        key1 = currentGroupId,
    ) {
        val flow = if (currentGroupId != null) viewModel.getGroupStandings(currentGroupId)
                   else emptyFlow()
        flow.collect { value = it }
    }

    Scaffold(
        topBar = {
            TopBar(
                actions = {
                    CalendarIconButton(
                        selectedDate = selectedDate,
                        onClick = { /* TODO: open date selector */ }
                    )
                }
            )
        }
    ) { innerPadding ->
        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No matches today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                DotIndicator(
                    pageCount = matches.size,
                    currentPage = pagerState.currentPage,
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        MatchCard(match = matches[page])
                    }
                }

                // Always show the GroupCard when a group is selected.
                // GroupCard itself handles the loading/empty state.
                if (currentGroupId != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    GroupCard(
                        groupId = currentGroupId,
                        standings = groupStandings,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
