package com.example.worldcup.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
                Status.LIVE      -> "🔴 ${match.minute}'"
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = countryCodeToFlagEmoji(match.homeTeam.flag), fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.homeTeam.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Center: score for live/finished, kickoff time for upcoming
                when (match.status) {
                    Status.UPCOMING -> Text(
                        text = formatKickoffTime(match),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Status.LIVE, Status.COMPLETED -> Text(
                        text = "${match.homeTeamScore}  –  ${match.awayTeamScore}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Away team
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = countryCodeToFlagEmoji(match.awayTeam.flag), fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.awayTeam.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
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
