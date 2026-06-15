package com.example.worldcup.ui.screens.groups

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worldcup.data.model.GroupStanding

// Column widths — keep header and data rows in sync
private val W_POS  = 20.dp
private val W_MP   = 26.dp
private val W_WIN  = 26.dp
private val W_DRW  = 26.dp
private val W_LST  = 26.dp
private val W_GF   = 26.dp
private val W_GA   = 26.dp
private val W_GD   = 30.dp
private val W_PTS  = 30.dp

private fun countryCodeToFlagEmoji(code: String): String =
    code.uppercase().map { 0x1F1E6 + (it - 'A') }
        .joinToString("") { String(Character.toChars(it)) }

@Composable
fun GroupCard(
    groupId: String,
    standings: List<GroupStanding>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Title ───────────────────────────────────────────────────────
            Text(
                text = "GROUP $groupId",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            HorizontalDivider()

            if (standings.isEmpty()) {
                // Placeholder while standings are loading or unavailable
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                // ── Header row ──────────────────────────────────────────────
                StandingsRow(
                    position   = "#",
                    teamLabel  = "Team",
                    mp = "MP", w = "W", d = "D", l = "L",
                    gf = "GF", ga = "GA", gd = "GD", pts = "Pts",
                    isHeader   = true,
                )

                HorizontalDivider()

                // ── Data rows ───────────────────────────────────────────────
                standings.forEachIndexed { index, s ->
                    StandingsRow(
                        position  = s.position.toString(),
                        teamLabel = "${countryCodeToFlagEmoji(s.team.flag)}  ${s.team.name}",
                        mp  = s.played.toString(),
                        w   = s.won.toString(),
                        d   = s.drawn.toString(),
                        l   = s.lost.toString(),
                        gf  = s.goalsFor.toString(),
                        ga  = s.goalsAgainst.toString(),
                        gd  = formatGd(s.goalDifference),
                        pts = s.points.toString(),
                        gdColor = gdColor(s.goalDifference),
                        ptsWeight = FontWeight.Bold,
                    )
                    if (index < standings.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun StandingsRow(
    position: String,
    teamLabel: String,
    mp: String, w: String, d: String, l: String,
    gf: String, ga: String, gd: String, pts: String,
    isHeader: Boolean = false,
    gdColor: Color = Color.Unspecified,
    ptsWeight: FontWeight = FontWeight.Normal,
) {
    val textColor = if (isHeader)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = if (isHeader) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cell(position, W_POS, color = textColor, align = TextAlign.Center)
        Spacer(Modifier.width(6.dp))

        Text(
            text = teamLabel,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(4.dp))

        Cell(mp,  W_MP,  color = textColor)
        Cell(w,   W_WIN, color = textColor)
        Cell(d,   W_DRW, color = textColor)
        Cell(l,   W_LST, color = textColor)
        Cell(gf,  W_GF,  color = textColor)
        Cell(ga,  W_GA,  color = textColor)
        Cell(gd,  W_GD,  color = if (isHeader) textColor else gdColor)
        Cell(pts, W_PTS, color = textColor, weight = ptsWeight)
    }
}

@Composable
private fun Cell(
    text: String,
    width: Dp,
    color: Color = Color.Unspecified,
    align: TextAlign = TextAlign.Center,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = MaterialTheme.typography.bodySmall,
        fontSize = 11.sp,
        color = color,
        textAlign = align,
        fontWeight = weight,
        maxLines = 1,
    )
}

private fun formatGd(gd: Int): String = when {
    gd > 0  -> "+$gd"
    else    -> gd.toString()
}

@Composable
private fun gdColor(gd: Int): Color = when {
    gd > 0  -> Color(0xFF2E7D32)
    gd < 0  -> MaterialTheme.colorScheme.error
    else    -> MaterialTheme.colorScheme.onSurfaceVariant
}
