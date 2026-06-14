package com.example.worldcup.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.worldcup.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    actions: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Image(
                painter = painterResource(R.drawable.world_cup_emblem),
                contentDescription = "FIFA World Cup 2026",
                modifier = Modifier.height(36.dp)
            )
        },
        actions = { actions() }
    )
}
