package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.StudyAnalytics
import com.example.data.model.StudyConstants
import com.example.ui.components.PageHeader
import com.example.ui.components.SectionPanel
import com.example.ui.components.SelectablePill
import com.example.ui.components.StudyProgressBar
import com.example.ui.components.WeightageChip
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.StudyDashboardUiState

@Composable
fun SyllabusScreen(
    state: StudyDashboardUiState,
    onClassExamChanged: (cls: String, exam: String) -> Unit,
    onChapterStatusChanged: (chapterId: String, status: String) -> Unit
) {
    var selectedSubject by remember { mutableStateOf("All") }
    val subjectsForExam = remember(state.profile.exam) {
        StudyConstants.subjectsForExam(state.profile.exam)
    }
    if (selectedSubject != "All" && selectedSubject !in subjectsForExam) {
        selectedSubject = "All"
    }

    val filteredChapters = remember(state.chapters, state.profile.cls, state.profile.exam) {
        StudyAnalytics.filteredChapters(state.chapters, state.profile)
    }
    val doneCount = remember(filteredChapters, state.chapterStatuses) {
        filteredChapters.count { state.chapterStatuses[it.id] in StudyConstants.DONE_STATUSES }
    }
    val progressPct = StudyAnalytics.pct(doneCount, filteredChapters.size)

    val visibleSubjects = remember(subjectsForExam, selectedSubject) {
        subjectsForExam.filter { selectedSubject == "All" || it == selectedSubject }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("syllabus_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                eyebrow = "Know your next step",
                title = "Syllabus tracker",
                subtitle = "Status: Not Started → In Progress → Revised → Mastered."
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CompactDropdownSelector(
                    label = "Class ${state.profile.cls}",
                    options = StudyConstants.CLASSES.map { "Class $it" to it },
                    onSelected = { newCls -> onClassExamChanged(newCls, state.profile.exam) },
                    modifier = Modifier.weight(1f).testTag("syllabus_class_selector")
                )
                CompactDropdownSelector(
                    label = "Exam: ${state.profile.exam}",
                    options = StudyConstants.EXAMS.map { it to it },
                    onSelected = { newExam -> onClassExamChanged(state.profile.cls, newExam) },
                    modifier = Modifier.weight(1f).testTag("syllabus_exam_selector")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (listOf("All") + subjectsForExam).forEach { subject ->
                    SelectablePill(
                        label = subject,
                        selected = selectedSubject == subject,
                        onClick = { selectedSubject = subject },
                        modifier = Modifier.testTag("syllabus_filter_$subject")
                    )
                }
            }
        }

        item {
            SectionPanel {
                Text(
                    text = "Class ${state.profile.cls} · ${state.profile.exam}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$progressPct% revised or mastered",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                StudyProgressBar(fraction = progressPct / 100f)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$doneCount of ${filteredChapters.size} chapters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        visibleSubjects.forEach { subject ->
            item(key = "subject_$subject") {
                val chaptersInSub = filteredChapters.filter { it.subject == subject }
                val subDone = chaptersInSub.count { state.chapterStatuses[it.id] in StudyConstants.DONE_STATUSES }
                SectionPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = subject, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "$subDone/${chaptersInSub.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chaptersInSub.forEach { chapter ->
                            val currentStatus = state.chapterStatuses[chapter.id] ?: "Not Started"
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = MonostudyPalette.PanelElevated,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = chapter.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        CompactDropdownSelector(
                                            label = currentStatus,
                                            options = StudyConstants.CHAPTER_STATUSES.map { it to it },
                                            onSelected = { onChapterStatusChanged(chapter.id, it) },
                                            modifier = Modifier.testTag("chapter_status_${chapter.id}")
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        WeightageChip(band = chapter.weightage, labelPrefix = "weightage")
                                        WeightageChip(
                                            band = chapter.past10YearQuestionFrequency.marker,
                                            labelPrefix = "10-yr freq (est.)"
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
            Text(
                text = stringResource(R.string.provisional_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompactDropdownSelector(
    label: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select option",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (display, value) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}
