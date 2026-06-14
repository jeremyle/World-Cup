package com.example.worldcup.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.worldcup.data.model.Match
import com.example.worldcup.data.model.Status
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Converts a 2-letter country code (e.g. "BR") to its flag emoji (e.g. 🇧🇷)
private fun countryCodeToFlagEmoji(code: String): String =
    code.uppercase().map { 0x1F1E6 + (it - 'A') }
        .joinToString("") { String(Character.toChars(it)) }

private fun formatKickoffTime(match: Match): String {
    val local = match.kickoffTime.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour   = local.hour % 12
    val h      = if (hour == 0) 12 else hour
    val m      = local.minute.toString().padStart(2, '0')
    val amPm   = if (local.hour < 12) "AM" else "PM"
    return "$h:$m $amPm"
}

@Composable
fun MatchCard(
    match: Match,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status chip: live minute, FT, or kickoff label
            val statusText = when (match.status) {
                Status.LIVE      -> if (match.minute != null) "🔴 ${match.minute}'" else "🔴 LIVE"
                Status.COMPLETED -> "FT"
                Status.UPCOMING  -> "Upcoming"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = if (match.status == Status.LIVE)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Teams and score / kickoff time
            // IntrinsicSize.Max makes all children the same height so the
            // center Box can fillMaxHeight() and vertically center its content.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top,
            ) {
                // Home team — flag always at top, name always 2 lines
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = countryCodeToFlagEmoji(match.homeTeam.flag), fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.homeTeam.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Center: fixed width so team columns always get equal space.
                // fillMaxHeight + Alignment.Center vertically centers the text.
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    when (match.status) {
                        Status.UPCOMING -> Text(
                            text = formatKickoffTime(match),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Status.LIVE, Status.COMPLETED -> Text(
                            text = "${match.homeTeamScore} – ${match.awayTeamScore}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Away team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = countryCodeToFlagEmoji(match.awayTeam.flag), fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.awayTeam.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        minLines = 2,
                        maxLines = 2,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stadium
            Text(
                text = "${match.stadium.name} · ${match.stadium.location.city}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
