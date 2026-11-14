package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PyqQuestion
import com.example.data.model.StudyConstants
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PageHeader
import com.example.ui.components.SectionPanel
import com.example.ui.components.SelectablePill
import com.example.ui.components.StudyChip
import com.example.ui.components.formatMathText
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.PyqAnswerOutcome
import com.example.ui.viewmodel.StudyDashboardUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PyqVaultScreen(
    state: StudyDashboardUiState,
    answeredPyqs: Map<String, PyqAnswerOutcome>,
    onQuestionVisible: (String) -> Unit,
    onAnswerSelected: (PyqQuestion, Int, String) -> Unit,
    onErrorCategorySelected: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterSubject by remember { mutableStateOf("") }
    var filterChapter by remember { mutableStateOf("") }
    var filterDifficulty by remember { mutableStateOf("") }
    var filterExam by remember { mutableStateOf("") }
    var filterTag by remember { mutableStateOf("") }

    val selectedConfidenceByQid = remember { mutableStateMapOf<String, String>() }

    val availableChapters = remember(state.allPyqs) {
        state.allPyqs.map { it.chapterId }.filter { it.isNotBlank() }.distinct()
    }
    val availableTags = remember(state.allPyqs) {
        state.allPyqs.flatMap { it.tags }.distinct().sorted()
    }

    val filteredQuestions = remember(
        state.allPyqs, searchQuery, filterSubject, filterChapter, filterDifficulty, filterExam, filterTag
    ) {
        state.allPyqs.filter { q ->
            (filterSubject.isEmpty() || q.subject == filterSubject) &&
                (filterChapter.isEmpty() || q.chapterId == filterChapter) &&
                (filterDifficulty.isEmpty() || q.difficulty == filterDifficulty) &&
                (filterExam.isEmpty() || q.exam == filterExam) &&
                (filterTag.isEmpty() || filterTag in q.tags) &&
                (searchQuery.isBlank() || (q.q + " " + q.tags.joinToString(" "))
                    .contains(searchQuery.trim(), ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("pyq_vault_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                eyebrow = "Past-paper practice",
                title = "PYQ vault",
                subtitle = "Search and filter questions, then log how confident you were."
            )
        }

        item {
            SectionPanel {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search questions or tags…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pyq_search_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompactDropdownSelector(
                        label = filterSubject.ifEmpty { "All subjects" },
                        options = listOf("All subjects" to "") + StudyConstants.SUBJECTS.map { it to it },
                        onSelected = { filterSubject = it },
                        modifier = Modifier.testTag("pyq_filter_subject")
                    )
                    CompactDropdownSelector(
                        label = if (filterChapter.isEmpty()) "All chapters" else (state.chapterMap[filterChapter]?.title ?: filterChapter),
                        options = listOf("All chapters" to "") + availableChapters.map { id ->
                            (state.chapterMap[id]?.title ?: id) to id
                        },
                        onSelected = { filterChapter = it },
                        modifier = Modifier.testTag("pyq_filter_chapter")
                    )
                    CompactDropdownSelector(
                        label = filterDifficulty.ifEmpty { "Any difficulty" },
                        options = listOf("Any difficulty" to "") + StudyConstants.DIFFICULTIES.map { it to it },
                        onSelected = { filterDifficulty = it },
                        modifier = Modifier.testTag("pyq_filter_difficulty")
                    )
                    CompactDropdownSelector(
                        label = filterExam.ifEmpty { "Any exam" },
                        options = listOf("Any exam" to "", "JEE" to "JEE", "NEET" to "NEET"),
                        onSelected = { filterExam = it },
                        modifier = Modifier.testTag("pyq_filter_exam")
                    )
                    CompactDropdownSelector(
                        label = filterTag.ifEmpty { "Any concept tag" },
                        options = listOf("Any concept tag" to "") + availableTags.map { it to it },
                        onSelected = { filterTag = it },
                        modifier = Modifier.testTag("pyq_filter_tag")
                    )
                }
                if (state.pyqNotice.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.pyqNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (filteredQuestions.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Outlined.Quiz,
                    message = "No questions match these filters."
                )
            }
        } else {
            items(filteredQuestions, key = { it.id }) { question ->
                LaunchedEffect(question.id) {
                    onQuestionVisible(question.id)
                }
                val outcome = answeredPyqs[question.id]
                val confidence = selectedConfidenceByQid[question.id] ?: "MEDIUM"
                val chapterTitle = state.chapterMap[question.chapterId]?.title.orEmpty()

                SectionPanel {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val examTag = buildString {
                            append(question.exam)
                            if (question.year != null) append(" ${question.year}") else append(" · practice")
                        }
                        StudyChip(text = examTag)
                        StudyChip(text = question.subject)
                        StudyChip(text = question.difficulty)
                        if (chapterTitle.isNotEmpty()) StudyChip(text = chapterTitle)
                        question.tags.forEach { tag -> StudyChip(text = "#$tag") }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = formatMathText(question.q),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Confidence:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StudyConstants.CONFIDENCE_LEVELS.forEach { level ->
                            val label = level.lowercase().replaceFirstChar { it.uppercase() }
                            SelectablePill(
                                label = label,
                                selected = confidence == level,
                                onClick = {
                                    if (outcome == null) {
                                        selectedConfidenceByQid[question.id] = level
                                    }
                                },
                                modifier = Modifier.testTag("conf_${question.id}_$level")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.forEachIndexed { idx, optText ->
                            val isCorrectOpt = idx == question.answer
                            val isSelectedOpt = outcome?.selectedOption == idx
                            val bgColor: Color
                            val borderColor: Color
                            when {
                                outcome == null -> {
                                    bgColor = MonostudyPalette.PanelElevated
                                    borderColor = MaterialTheme.colorScheme.outline
                                }
                                isCorrectOpt -> {
                                    bgColor = MonostudyPalette.AccentGreen.copy(alpha = 0.16f)
                                    borderColor = MonostudyPalette.AccentGreen
                                }
                                isSelectedOpt -> {
                                    bgColor = MonostudyPalette.AccentRed.copy(alpha = 0.16f)
                                    borderColor = MonostudyPalette.AccentRed
                                }
                                else -> {
                                    bgColor = MonostudyPalette.PanelElevated.copy(alpha = 0.6f)
                                    borderColor = MaterialTheme.colorScheme.outline
                                }
                            }
                            Surface(
                                onClick = {
                                    if (outcome == null) {
                                        onAnswerSelected(question, idx, confidence)
                                    }
                                },
                                enabled = outcome == null,
                                shape = RoundedCornerShape(10.dp),
                                color = bgColor,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("pyq_opt_${question.id}_$idx")
                            ) {
                                Text(
                                    text = "${"ABCD"[idx]}. ${formatMathText(optText)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }

                    if (outcome != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MonostudyPalette.PanelElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(
                                    text = if (outcome.correct) {
                                        "✓ Correct · ${outcome.secs}s"
                                    } else {
                                        "✗ Not quite — added to your Error Log · ${outcome.secs}s"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (outcome.correct) MonostudyPalette.AccentGreen else MonostudyPalette.AccentRed,
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

                        if (!outcome.correct) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Why did you miss it?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StudyConstants.ERROR_CATEGORIES.forEach { cat ->
                                    SelectablePill(
                                        label = cat,
                                        selected = outcome.selectedErrorCategory == cat,
                                        onClick = { onErrorCategorySelected(question.id, cat) },
                                        modifier = Modifier.testTag("err_cat_${question.id}_$cat")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
