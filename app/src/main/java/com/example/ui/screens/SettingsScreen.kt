package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.StudyConstants
import com.example.ui.components.PageHeader
import com.example.ui.components.SectionPanel
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.StudyDashboardUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: StudyDashboardUiState,
    userEmail: String?,
    onSaveProfile: (name: String, cls: String, exam: String, goalMinutes: Long) -> Unit,
    onBuildBackupJson: () -> String,
    onImportBackupJson: (String) -> Unit,
    onBuildQuestionTemplateJson: () -> String,
    onImportQuestionsJson: (String) -> Unit,
    onClearCustomQuestions: () -> Unit,
    onResetAllData: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(state.profile.name) }
    var cls by remember { mutableStateOf(state.profile.cls) }
    var exam by remember { mutableStateOf(state.profile.exam) }
    var goalText by remember { mutableStateOf(state.profile.goalMinutes.toString()) }

    LaunchedEffect(state.profile) {
        name = state.profile.name
        cls = state.profile.cls
        exam = state.profile.exam
        goalText = state.profile.goalMinutes.toString()
    }

    var showClearQuestionsConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) writeTextToUri(context, uri, onBuildBackupJson())
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) readTextFromUri(context, uri)?.let(onImportBackupJson)
    }

    val exportTemplateLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) writeTextToUri(context, uri, onBuildQuestionTemplateJson())
    }

    val importQuestionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) readTextFromUri(context, uri)?.let(onImportQuestionsJson)
    }

    if (showClearQuestionsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearQuestionsConfirm = false },
            title = { Text("Remove imported questions?") },
            text = { Text("Your attempts stay, but those questions disappear from the vault.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearQuestionsConfirm = false
                        onClearCustomQuestions()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MonostudyPalette.AccentRed)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showClearQuestionsConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all data?") },
            text = { Text("Progress, notes, attempts and tasks in your account will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MonostudyPalette.AccentRed)
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                eyebrow = "Your space, your goals",
                title = "Profile & settings",
                subtitle = "Synced with your Google account."
            )
        }

        item {
            SectionPanel {
                Text(text = "Student profile", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Display name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("settings_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompactDropdownSelector(
                        label = "Class $cls",
                        options = StudyConstants.CLASSES.map { "Class $it" to it },
                        onSelected = { cls = it },
                        modifier = Modifier.weight(1f).testTag("settings_class_selector")
                    )
                    CompactDropdownSelector(
                        label = "Exam: $exam",
                        options = StudyConstants.EXAMS.map { it to it },
                        onSelected = { exam = it },
                        modifier = Modifier.weight(1f).testTag("settings_exam_selector")
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Daily goal (minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("settings_goal_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onSaveProfile(name, cls, exam, goalText.toLongOrNull() ?: 0L) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_save_profile_button")
                ) {
                    Text("Save profile")
                }
            }
        }

        item {
            SectionPanel {
                Text(text = "Backups & custom questions", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportBackupLauncher.launch("monostudy-backup.json") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("export_backup_button")
                    ) { Text("Export backup") }
                    OutlinedButton(
                        onClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("import_backup_button")
                    ) { Text("Import backup") }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { importQuestionsLauncher.launch(arrayOf("application/json", "text/plain")) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("import_questions_button")
                    ) { Text("Import questions (.json)") }
                    OutlinedButton(
                        onClick = { exportTemplateLauncher.launch("monostudy-questions-template.json") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("question_template_button")
                    ) { Text("Question file template") }
                    OutlinedButton(
                        onClick = { if (state.customPyqs.isNotEmpty()) showClearQuestionsConfirm = true },
                        enabled = state.customPyqs.isNotEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("remove_imported_button")
                    ) { Text("Remove imported") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${state.customPyqs.size} imported question(s) · ${state.basePyqs.size} built in",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SectionPanel {
                Text(text = "Account & danger zone", style = MaterialTheme.typography.titleLarge)
                if (!userEmail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Signed in as $userEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MonostudyPalette.AccentRed),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_logout_button")
                    ) {
                        Text("Log out")
                    }
                    Button(
                        onClick = { showResetConfirm = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MonostudyPalette.AccentRed,
                            contentColor = androidx.compose.ui.graphics.Color(0xFF1A0509)
                        ),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_reset_button")
                    ) {
                        Text("Reset all data")
                    }
                }
            }
        }
    }
}

private fun writeTextToUri(context: Context, uri: Uri, content: String) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
    }
}

private fun readTextFromUri(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }.getOrNull()
