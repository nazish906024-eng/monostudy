package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyAnalytics
import com.example.data.model.StudyConstants
import com.example.data.model.StudyTask
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PageHeader
import com.example.ui.components.PanelHeader
import com.example.ui.components.SectionPanel
import com.example.ui.components.SelectablePill
import com.example.ui.components.StudyProgressBar
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.PomodoroUiState
import com.example.ui.viewmodel.StudyDashboardUiState

@Composable
fun PlannerScreen(
    state: StudyDashboardUiState,
    pomodoro: PomodoroUiState,
    onAddTask: (text: String, subject: String) -> Unit,
    onToggleTask: (StudyTask) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearCompletedTasks: () -> Unit,
    onPomodoroTagChange: (String) -> Unit,
    onTogglePomodoroRun: () -> Unit,
    onResetPomodoro: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onSaveGoal: (Long) -> Unit,
    onLog25Minutes: (String) -> Unit
) {
    var taskText by remember { mutableStateOf("") }
    var taskSubject by remember { mutableStateOf("General") }
    var goalText by remember { mutableStateOf(state.profile.goalMinutes.toString()) }

    LaunchedEffect(state.profile.goalMinutes) {
        goalText = state.profile.goalMinutes.toString()
    }

    val today = remember { StudyAnalytics.todayKey() }
    val minutesToday = remember(state.sessions, today) {
        StudyAnalytics.minutesOnDate(state.sessions, today)
    }
    val goalMinutes = state.profile.goalMinutes.toInt().coerceAtLeast(15)

    fun submitTask() {
        val clean = taskText.trim()
        if (clean.isNotEmpty()) {
            onAddTask(clean, taskSubject)
            taskText = ""
        } else {
            onAddTask("", taskSubject)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("planner_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                eyebrow = "Make time for what matters",
                title = "Study planner",
                subtitle = "Plan your day, tag your focus sessions, hit your goal."
            )
        }

        // Daily Checklist
        item {
            SectionPanel {
                PanelHeader(
                    eyebrow = "Daily checklist",
                    title = "Your tasks",
                    actionLabel = if (state.tasks.any { it.done }) "Clear completed" else null,
                    onActionClick = onClearCompletedTasks
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = taskText,
                    onValueChange = { taskText = it.take(120) },
                    placeholder = { Text("e.g. Revise Newton’s laws") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submitTask() }),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("planner_task_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactDropdownSelector(
                        label = taskSubject,
                        options = StudyConstants.TASK_SUBJECTS.map { it to it },
                        onSelected = { taskSubject = it },
                        modifier = Modifier.testTag("planner_task_subject")
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { submitTask() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("planner_add_task_button")
                    ) {
                        Text("Add")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (state.tasks.isEmpty()) {
                    EmptyStatePanel(
                        icon = Icons.Outlined.CheckCircle,
                        message = "No tasks yet. Add one small, clear task."
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.tasks.forEach { task ->
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

        // Pomodoro Timer
        item {
            SectionPanel {
                Text(
                    text = "FOCUS SESSION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = "Pomodoro timer", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pomodoro.tag,
                    onValueChange = { onPomodoroTagChange(it.take(80)) },
                    placeholder = { Text("Tag, e.g. 50m • Electrostatics PYQs") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pomodoro_tag_input")
                )

                Spacer(modifier = Modifier.height(18.dp))

                val totalSeconds = (pomodoro.presetMinutes * 60).coerceAtLeast(1)
                val progress = pomodoro.secondsLeft.toFloat() / totalSeconds.toFloat()
                val mm = (pomodoro.secondsLeft / 60).toString().padStart(2, '0')
                val ss = (pomodoro.secondsLeft % 60).toString().padStart(2, '0')

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(196.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                            drawArc(
                                color = MonostudyPalette.PanelElevated,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = MonostudyPalette.Primary,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$mm:$ss",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = pomodoro.statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTogglePomodoroRun,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("pomodoro_toggle_button")
                    ) {
                        val label = when {
                            pomodoro.isRunning -> "Pause"
                            pomodoro.secondsLeft < pomodoro.presetMinutes * 60 -> "Resume"
                            else -> "Start focus"
                        }
                        Text(label)
                    }
                    OutlinedButton(
                        onClick = onResetPomodoro,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("pomodoro_reset_button")
                    ) {
                        Text("Reset")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = pomodoro.soundEnabled,
                            onCheckedChange = onToggleSound,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("pomodoro_sound_toggle")
                        )
                        Text(
                            text = "sound",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    StudyConstants.POMODORO_PRESETS.forEach { mins ->
                        SelectablePill(
                            label = "$mins min",
                            selected = pomodoro.presetMinutes == mins,
                            onClick = { onSelectPreset(mins) },
                            modifier = Modifier.testTag("pomodoro_preset_$mins")
                        )
                    }
                }
            }
        }

        // Daily Target
        item {
            SectionPanel {
                Text(
                    text = "DAILY TARGET",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = "Study goal", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter { ch -> ch.isDigit() }.take(3) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.width(100.dp).testTag("planner_goal_input")
                    )
                    Text(
                        text = "min/day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            val parsed = goalText.toLongOrNull() ?: 0L
                            onSaveGoal(parsed)
                        },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("planner_goal_save_button")
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = "Save goal", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                StudyProgressBar(
                    fraction = minutesToday.toFloat() / goalMinutes.toFloat(),
                    color = MonostudyPalette.AccentBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$minutesToday of $goalMinutes minutes today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = { onLog25Minutes(pomodoro.tag) },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("planner_log_25_button")
                ) {
                    Text("＋ Log 25 minutes manually")
                }
            }
        }
    }
}
