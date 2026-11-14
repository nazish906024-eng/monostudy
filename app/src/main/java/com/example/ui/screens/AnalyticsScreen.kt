package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.StudyAnalytics
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PageHeader
import com.example.ui.components.SectionPanel
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StudyChip
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.StudyDashboardUiState
import kotlinx.coroutines.launch
import java.io.File
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    state: StudyDashboardUiState,
    onGenerateWeeklyCard: suspend (android.content.Context) -> File
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filteredChapters = remember(state.chapters, state.profile.cls, state.profile.exam) {
        StudyAnalytics.filteredChapters(state.chapters, state.profile)
    }
    val chapterAcc = remember(state.attempts) {
        StudyAnalytics.chapterAccuracyMap(state.attempts)
    }
    val overallAcc = remember(state.attempts) {
        StudyAnalytics.overallAccuracyPct(state.attempts)
    }
    val totalMinutes = remember(state.sessions) {
        state.sessions.sumOf { it.minutes.toInt() }
    }
    val streakInfo = remember(state.sessions, state.attempts, state.tasks) {
        StudyAnalytics.streak(state.sessions, state.attempts, state.tasks)
    }
    val weakSpots = remember(filteredChapters, state.chapterStatuses, state.attempts) {
        StudyAnalytics.weakSpots(filteredChapters, state.chapterStatuses, state.attempts)
    }
    val last7Days = remember { StudyAnalytics.lastNDays(7) }
    val dayMinutes = remember(state.sessions, last7Days) {
        last7Days.map { day ->
            val key = StudyAnalytics.todayKey(day)
            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to
                StudyAnalytics.minutesOnDate(state.sessions, key)
        }
    }
    val goalMinutes = state.profile.goalMinutes.toInt().coerceAtLeast(15)
    val maxChartVal = remember(dayMinutes, goalMinutes) {
        max(goalMinutes, dayMinutes.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    }
    val last7Keys = remember(last7Days) { last7Days.map { StudyAnalytics.todayKey(it) }.toSet() }
    val sessionsByTag = remember(state.sessions, last7Keys) {
        state.sessions
            .filter { it.dateKey in last7Keys }
            .groupBy { it.tag.ifBlank { "Untagged" } }
            .mapValues { (_, list) -> list.sumOf { it.minutes.toInt() } }
            .entries
            .sortedByDescending { it.value }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                eyebrow = "Measure what you practise",
                title = "Performance analytics",
                subtitle = "Based on your synced study and practice activity.",
                trailing = {
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    val pngFile = onGenerateWeeklyCard(context)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pngFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Weekly Summary Card"))
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("weekly_summary_button")
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Weekly card")
                    }
                }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatMetricCard(
                        label = "Attempts",
                        value = "${state.attempts.size}",
                        caption = "Total PYQs logged",
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "Accuracy",
                        value = overallAcc?.let { "$it%" } ?: "—",
                        caption = "Overall PYQ score",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatMetricCard(
                        label = "Minutes logged",
                        value = "$totalMinutes",
                        caption = "All focus sessions",
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "Streak (1 grace day)",
                        value = "🔥 ${streakInfo.days}",
                        caption = if (streakInfo.gracesUsed > 0) "${streakInfo.gracesUsed} grace day(s) used" else "no grace used",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Weak-spot heatmap
        item {
            SectionPanel {
                Text(text = "Weak-spot heatmap", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Accuracy per chapter · grey = no attempts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredChapters.forEach { chapter ->
                        val acc = chapterAcc[chapter.id]?.accuracyPct
                        val bg = when {
                            acc == null -> MonostudyPalette.PanelElevated
                            acc < 40 -> MonostudyPalette.HeatLow
                            acc < 60 -> MonostudyPalette.HeatMidLow
                            acc < 80 -> MonostudyPalette.HeatMidHigh
                            else -> MonostudyPalette.HeatHigh
                        }
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = bg,
                            modifier = Modifier.width(108.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = acc?.let { "$it%" } ?: "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // High yield low accuracy
        item {
            SectionPanel {
                Text(text = "High yield • low accuracy", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "High-weightage chapters scoring under 60%, or not yet revised.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (weakSpots.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Outlined.Analytics,
                        message = "No weak spots detected yet."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        weakSpots.take(8).forEach { spot ->
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = MonostudyPalette.PanelElevated,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = spot.chapter.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    StudyChip(text = spot.chapter.subject)
                                    StudyChip(
                                        text = spot.accuracyPct?.let { "$it%" } ?: "untested",
                                        borderColor = MonostudyPalette.AccentRed,
                                        textColor = MonostudyPalette.AccentRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7-day study time vs goal bar chart
        item {
            SectionPanel {
                Text(text = "Study time vs goal (7 days)", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp, top = 16.dp)) {
                        val barCount = dayMinutes.size.coerceAtLeast(1)
                        val slotWidth = size.width / barCount
                        val barWidth = slotWidth * 0.55f

                        dayMinutes.forEachIndexed { idx, (_, mins) ->
                            val frac = (mins.toFloat() / maxChartVal.toFloat()).coerceIn(0f, 1f)
                            val barHeight = max(4.dp.toPx(), size.height * frac)
                            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                            drawRoundRect(
                                color = MonostudyPalette.Primary,
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }

                        val goalFrac = (goalMinutes.toFloat() / maxChartVal.toFloat()).coerceIn(0f, 1f)
                        val goalY = size.height * (1f - goalFrac)
                        drawLine(
                            color = MonostudyPalette.AccentOrange,
                            start = Offset(0f, goalY),
                            end = Offset(size.width, goalY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dayMinutes.forEach { (dayLabel, mins) ->
                            Column(
                                modifier = Modifier.weight(1f).fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$mins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Dashed line = daily goal ($goalMinutes min).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Focus sessions by tag
        item {
            SectionPanel {
                Text(text = "Focus sessions by tag", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                if (sessionsByTag.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Outlined.Timer,
                        message = "Tag a Pomodoro session to see it here."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sessionsByTag.forEach { (tag, mins) ->
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = MonostudyPalette.PanelElevated,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = tag, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    StudyChip(text = "$mins min")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
