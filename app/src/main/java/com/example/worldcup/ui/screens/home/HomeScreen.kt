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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.worldcup.data.StubData
import com.example.worldcup.ui.components.CalendarIconButton
import com.example.worldcup.ui.components.TopBar
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun HomeScreen(viewModel: HomeScreenViewModel = viewModel()) {
    val matches = StubData.matches
    val pagerState = rememberPagerState(pageCount = { matches.size })
    val selectedDate = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    MatchCard(match = matches[page])
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DotIndicator(
                pageCount = matches.size,
                currentPage = pagerState.currentPage
            )
        }
    }
}
