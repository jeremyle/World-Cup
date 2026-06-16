package com.example.worldcup.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.worldcup.data.model.PlayerStat

@Composable
fun PlayerStatsCard(
    topScorers: List<PlayerStat>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier  = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Header
            Text(
                text       = "Top Scorers",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            HorizontalDivider()

            if (topScorers.isEmpty()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "No data yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    topScorers.take(10).forEachIndexed { index, player ->
                        val isFirst = index == 0 || topScorers[index - 1].goals != player.goals
                        val displayRank = if (isFirst) "${player.rank}" else "T${player.rank}"
                        PlayerStatRow(player = player, displayRank = displayRank)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerStatRow(
    player: PlayerStat,
    displayRank: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Rank — wider to fit "T10"
        Text(
            text       = displayRank,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier   = Modifier.width(32.dp),
        )

        // Avatar
        if (player.photoUrl.isNotEmpty()) {
            AsyncImage(
                model              = player.photoUrl,
                contentDescription = player.playerName,
                modifier           = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        } else {
            PlayerInitialsAvatar(
                name     = player.playerName,
                modifier = Modifier.size(42.dp),
            )
        }

        // Name + meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = player.playerName,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text     = flagEmoji(player.teamName),
                    fontSize = 14.sp,
                )
                Text(
                    text     = buildString {
                        if (player.age > 0) append("${player.age}y")
                        if (player.age > 0 && player.teamName.isNotEmpty()) append(" · ")
                        if (player.teamName.isNotEmpty()) append(player.teamName)
                    },
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Soccer ball badge with goal count
        StatBadge(stat = player.goals)
    }
}

@Composable
private fun StatBadge(stat: Int, modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text     = "⚽",
            fontSize = 38.sp,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = "$stat",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

// ── Initials avatar ───────────────────────────────────────────────────────────

@Composable
private fun PlayerInitialsAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = remember(name) {
        name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    }
    val avatarColor = remember(name) { nameToColor(name) }

    Box(
        modifier         = modifier
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = initials,
            color      = Color.White,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize   = 13.sp,
        )
    }
}

private fun nameToColor(name: String): Color {
    val hash = name.hashCode()
    val hue  = ((hash and 0xFF_FFFF) % 360).toFloat()
    return hslToColor(hue, saturation = 0.45f, lightness = 0.45f)
}

private fun hslToColor(h: Float, saturation: Float, lightness: Float): Color {
    val c  = (1f - kotlin.math.abs(2f * lightness - 1f)) * saturation
    val x  = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
    val m  = lightness - c / 2f
    val (r, g, b) = when {
        h < 60f  -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else     -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

// ── Country flag emoji ────────────────────────────────────────────────────────

private fun flagEmoji(teamName: String): String {
    val iso = NATIONALITY_TO_ISO[teamName] ?: return ""
    return iso.map { char ->
        String(Character.toChars(0x1F1E6 + (char.code - 'A'.code)))
    }.joinToString("")
}

/** Keys are exact team names from worldcup_2026.json (the local DB seed). */
private val NATIONALITY_TO_ISO = mapOf(
    "Algeria"        to "DZ",
    "Argentina"      to "AR",
    "Australia"      to "AU",
    "Austria"        to "AT",
    "Belgium"        to "BE",
    "Bosnia & Herz." to "BA",
    "Brazil"         to "BR",
    "Canada"         to "CA",
    "Cape Verde"     to "CV",
    "Colombia"       to "CO",
    "Croatia"        to "HR",
    "Curaçao"        to "CW",
    "Czechia"        to "CZ",
    "Côte d'Ivoire"  to "CI",
    "DR Congo"       to "CD",
    "Ecuador"        to "EC",
    "Egypt"          to "EG",
    "England"        to "GB",
    "France"         to "FR",
    "Germany"        to "DE",
    "Ghana"          to "GH",
    "Haiti"          to "HT",
    "Iran"           to "IR",
    "Iraq"           to "IQ",
    "Japan"          to "JP",
    "Jordan"         to "JO",
    "Mexico"         to "MX",
    "Morocco"        to "MA",
    "Netherlands"    to "NL",
    "New Zealand"    to "NZ",
    "Norway"         to "NO",
    "Panama"         to "PA",
    "Paraguay"       to "PY",
    "Portugal"       to "PT",
    "Qatar"          to "QA",
    "Saudi Arabia"   to "SA",
    "Scotland"       to "GB",
    "Senegal"        to "SN",
    "South Africa"   to "ZA",
    "South Korea"    to "KR",
    "Spain"          to "ES",
    "Sweden"         to "SE",
    "Switzerland"    to "CH",
    "Tunisia"        to "TN",
    "Türkiye"        to "TR",
    "United States"  to "US",
    "Uruguay"        to "UY",
    "Uzbekistan"     to "UZ",
)
