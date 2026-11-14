package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ErrorLogEntry
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PageHeader
import com.example.ui.components.SectionPanel
import com.example.ui.components.SelectablePill
import com.example.ui.components.StudyChip
import com.example.ui.components.formatMathText
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.StudyDashboardUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ErrorLogScreen(
    state: StudyDashboardUiState,
    onToggleResolved: (ErrorLogEntry) -> Unit,
    onDeleteError: (String) -> Unit
) {
    var showResolved by remember { mutableStateOf(false) }

    val pyqMap = remember(state.allPyqs) { state.allPyqs.associateBy { it.id } }
    val filteredErrors = remember(state.errors, showResolved, pyqMap) {
        state.errors.filter { it.resolved == showResolved && pyqMap.containsKey(it.qid) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("error_log_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                eyebrow = "Learn from mistakes",
                title = "Error log vault",
                subtitle = "Questions you got wrong during practice.",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectablePill(
                            label = "Open",
                            selected = !showResolved,
                            onClick = { showResolved = false },
                            modifier = Modifier.testTag("error_tab_open")
                        )
                        SelectablePill(
                            label = "Resolved",
                            selected = showResolved,
                            onClick = { showResolved = true },
                            modifier = Modifier.testTag("error_tab_resolved")
                        )
                    }
                }
            )
        }

        if (filteredErrors.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.Flag,
                    message = if (!showResolved) {
                        "No open errors. Wrong answers in the PYQ vault land here."
                    } else {
                        "Nothing resolved yet."
                    }
                )
            }
        } else {
            items(filteredErrors, key = { it.id }) { entry ->
                val question = pyqMap[entry.qid] ?: return@items
                val chapterTitle = state.chapterMap[question.chapterId]?.title.orEmpty()

                SectionPanel {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StudyChip(text = question.subject)
                        if (chapterTitle.isNotEmpty()) StudyChip(text = chapterTitle)
                        if (entry.category.isNotEmpty()) {
                            StudyChip(
                                text = entry.category,
                                borderColor = MonostudyPalette.AccentRed,
                                textColor = MonostudyPalette.AccentRed
                            )
                        }
                        if (entry.dateKey.isNotEmpty()) StudyChip(text = entry.dateKey)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = formatMathText(question.q),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MonostudyPalette.PanelElevated,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            val ansText = question.options.getOrNull(question.answer).orEmpty()
                            Text(
                                text = "Answer: ${"ABCD"[question.answer]}. ${formatMathText(ansText)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MonostudyPalette.AccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                            if (question.explain.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatMathText(question.explain),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { onToggleResolved(entry) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("error_resolve_${entry.id}")
                        ) {
                            Text(if (entry.resolved) "Reopen" else "Mark resolved")
                        }
                        OutlinedButton(
                            onClick = { onDeleteError(entry.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MonostudyPalette.AccentRed),
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("error_delete_${entry.id}")
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}
