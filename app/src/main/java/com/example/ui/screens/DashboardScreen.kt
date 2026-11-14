package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StudyAnalytics
import com.example.data.model.StudyConstants
import com.example.data.model.StudyTask
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PageHeader
import com.example.ui.components.PanelHeader
import com.example.ui.components.SectionPanel
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StudyChip
import com.example.ui.components.WeightageChip
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.StudyDashboardUiState

@Composable
fun DashboardScreen(
    state: StudyDashboardUiState,
    isWideScreen: Boolean,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSyllabus: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val today = remember { StudyAnalytics.todayKey() }
    val filteredChapters = remember(state.chapters, state.profile.cls, state.profile.exam) {
        StudyAnalytics.filteredChapters(state.chapters, state.profile)
    }
    val doneChaptersCount = remember(filteredChapters, state.chapterStatuses) {
        filteredChapters.count { state.chapterStatuses[it.id] in StudyConstants.DONE_STATUSES }
    }
    val syllabusPct = StudyAnalytics.pct(doneChaptersCount, filteredChapters.size)
    val minutesToday = remember(state.sessions, today) {
        StudyAnalytics.minutesOnDate(state.sessions, today)
    }
    val goalMinutes = state.profile.goalMinutes.toInt().coerceAtLeast(15)
    val openOrTodayTasks = remember(state.tasks, today) {
        state.tasks.filter { it.date == today || !it.done }
    }
    val tasksDoneCount = openOrTodayTasks.count { it.done }
    val overallAccuracy = remember(state.attempts) {
        StudyAnalytics.overallAccuracyPct(state.attempts)
    }
    val streakInfo = remember(state.sessions, state.attempts, state.tasks) {
        StudyAnalytics.streak(state.sessions, state.attempts, state.tasks)
    }
    val weakSpots = remember(filteredChapters, state.chapterStatuses, state.attempts) {
        StudyAnalytics.weakSpots(filteredChapters, state.chapterStatuses, state.attempts).take(3)
    }
    val nextChapters = remember(filteredChapters, state.chapterStatuses) {
        val inProg = filteredChapters.filter { (state.chapterStatuses[it.id] ?: "Not Started") == "In Progress" }
        val highYieldNew = filteredChapters.filter {
            (state.chapterStatuses[it.id] ?: "Not Started") == "Not Started" && it.weightage == "High"
        }
        (inProg + highYieldNew).take(4)
    }

    val displayName = state.profile.name.trim().ifEmpty { "Student" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                eyebrow = "Your prep journey",
                title = "Let’s make today count, $displayName.",
                subtitle = "A focused plan today brings you closer to your target."
            )
        }

        item {
            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        label = "Syllabus revised/mastered",
                        value = "$syllabusPct%",
                        caption = "$doneChaptersCount of ${filteredChapters.size} chapters",
                        progressFraction = syllabusPct / 100f,
                        accentColor = MonostudyPalette.Primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "Study time today",
                        value = "${minutesToday}m",
                        caption = "Goal $goalMinutes min",
                        progressFraction = minutesToday.toFloat() / goalMinutes.toFloat(),
                        accentColor = MonostudyPalette.AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "Tasks completed",
                        value = "$tasksDoneCount/${openOrTodayTasks.size}",
                        caption = "Open tasks carry over",
                        progressFraction = if (openOrTodayTasks.isEmpty()) 0f else tasksDoneCount.toFloat() / openOrTodayTasks.size,
                        accentColor = MonostudyPalette.AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "PYQ accuracy",
                        value = overallAccuracy?.let { "$it%" } ?: "—",
                        caption = "${state.attempts.size} attempts · ${streakInfo.days}-day streak",
                        progressFraction = (overallAccuracy ?: 0) / 100f,
                        accentColor = MonostudyPalette.AccentOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatMetricCard(
                            label = "Syllabus revised",
                            value = "$syllabusPct%",
                            caption = "$doneChaptersCount of ${filteredChapters.size} chapters",
                            progressFraction = syllabusPct / 100f,
                            accentColor = MonostudyPalette.Primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = "Study time today",
                            value = "${minutesToday}m",
                            caption = "Goal $goalMinutes min",
                            progressFraction = minutesToday.toFloat() / goalMinutes.toFloat(),
                            accentColor = MonostudyPalette.AccentBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatMetricCard(
                            label = "Tasks completed",
                            value = "$tasksDoneCount/${openOrTodayTasks.size}",
                            caption = "Open tasks carry over",
                            progressFraction = if (openOrTodayTasks.isEmpty()) 0f else tasksDoneCount.toFloat() / openOrTodayTasks.size,
                            accentColor = MonostudyPalette.AccentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            label = "PYQ accuracy",
                            value = overallAccuracy?.let { "$it%" } ?: "—",
                            caption = "${state.attempts.size} attempts · ${streakInfo.days}d streak",
                            progressFraction = (overallAccuracy ?: 0) / 100f,
                            accentColor = MonostudyPalette.AccentOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            SectionPanel {
                PanelHeader(
                    eyebrow = "Revise next",
                    title = "High yield · low accuracy",
                    actionLabel = "Analytics",
                    onActionClick = onNavigateToAnalytics
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (weakSpots.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Outlined.TrendingUp,
                        message = "Practice a few PYQs to surface your weak spots."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        weakSpots.forEach { spot ->
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
                                    if (spot.accuracyPct == null) {
                                        StudyChip(text = "untested")
                                    } else {
                                        StudyChip(
                                            text = "${spot.accuracyPct}%",
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
        }

        item {
            SectionPanel {
                PanelHeader(
                    eyebrow = "Pick up where you left off",
                    title = "Continue learning",
                    actionLabel = "All chapters",
                    onActionClick = onNavigateToSyllabus
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (nextChapters.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Outlined.AutoAwesome,
                        message = "Nothing queued. Nice work!"
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        nextChapters.forEach { chapter ->
                            val status = state.chapterStatuses[chapter.id] ?: "Not Started"
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
                                        text = chapter.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    WeightageChip(band = chapter.weightage)
                                    StudyChip(text = status)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionPanel {
                PanelHeader(
                    eyebrow = "Stay on track",
                    title = "Today’s plan",
                    actionLabel = "Planner",
                    onActionClick = onNavigateToPlanner
                )
                Spacer(modifier = Modifier.height(10.dp))
                val previewTasks = openOrTodayTasks.take(5)
                if (previewTasks.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Outlined.CheckCircle,
                        message = "No tasks yet — add one in the planner."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        previewTasks.forEach { task ->
                            TaskItemRow(
                                task = task,
                                onToggle = { onToggleTask(task) },
                                onDelete = { onDeleteTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemRow(
    task: StudyTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        color = MonostudyPalette.PanelElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("task_checkbox_${task.id}")
            )
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            StudyChip(text = task.subject)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete task",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
