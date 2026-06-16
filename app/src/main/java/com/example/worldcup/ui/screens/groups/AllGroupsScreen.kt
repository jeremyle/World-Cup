package com.example.worldcup.ui.screens.groups

import com.example.worldcup.ui.util.PredictiveBackContainer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.worldcup.data.model.GroupStanding
import com.example.worldcup.ui.screens.home.HomeScreenViewModel

private val GROUP_IDS = ('A'..'L').map { it.toString() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllGroupsScreen(
    currentGroupId: String?,
    viewModel: HomeScreenViewModel,
    onBack: () -> Unit,
) {
    val qualifyingThirdPlaceIds by viewModel.qualifyingThirdPlaceIds.collectAsState()
    val listState = rememberLazyListState()

    // Scroll to the current group on first composition
    LaunchedEffect(currentGroupId) {
        val index = GROUP_IDS.indexOfFirst { it == currentGroupId }
        if (index >= 0) listState.scrollToItem(index)
    }

    PredictiveBackContainer(onBack = onBack) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "All Groups",
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
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            itemsIndexed(GROUP_IDS) { _, groupId ->
                val standings by produceState<List<GroupStanding>>(
                    initialValue = emptyList(),
                    key1 = groupId,
                ) {
                    viewModel.getGroupStandings(groupId).collect { value = it }
                }

                GroupCard(
                    groupId = groupId,
                    standings = standings,
                    qualifyingThirdPlaceIds = qualifyingThirdPlaceIds,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
    } // PredictiveBackContainer
}
