package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.model.StudyConstants
import com.example.data.model.StudyNote
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PageHeader
import com.example.ui.components.SectionPanel
import com.example.ui.components.formatMathText
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.StudyDashboardUiState

@Composable
fun NotesScreen(
    state: StudyDashboardUiState,
    isWideScreen: Boolean,
    onCreateNote: (onCreated: (String) -> Unit) -> Unit,
    onUpdateNote: (StudyNote, String, String, String) -> Unit,
    onDeleteNote: (StudyNote, onDeleted: () -> Unit) -> Unit,
    onAttachImage: (android.content.Context, StudyNote, Uri) -> Unit,
    onRemoveImage: (StudyNote, String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }
    var isPreview by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selectedNote = remember(state.notes, selectedNoteId) {
        state.notes.find { it.id == selectedNoteId }
    }

    val filteredNotes = remember(state.notes, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) state.notes
        else state.notes.filter {
            it.title.contains(q, ignoreCase = true) || it.body.contains(q, ignoreCase = true)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && selectedNote != null) {
            onAttachImage(context, selectedNote, uri)
        }
    }

    if (!isWideScreen && selectedNote != null) {
        BackHandler {
            selectedNoteId = null
            isPreview = false
        }
    }

    if (showDeleteConfirm && selectedNote != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteNote(selectedNote) {
                            selectedNoteId = null
                            isPreview = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MonostudyPalette.AccentRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).testTag("notes_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader(
            eyebrow = "Personal knowledge base",
            title = "Notes & formulas",
            subtitle = "Markdown supported. Use $…$ for formulas. Attach images for diagrams.",
            trailing = {
                Button(
                    onClick = {
                        onCreateNote { newId ->
                            selectedNoteId = newId
                            isPreview = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("new_note_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New note")
                }
            }
        )

        if (isWideScreen) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NotesListPane(
                    notes = filteredNotes,
                    selectedNoteId = selectedNoteId,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onSelectNote = {
                        selectedNoteId = it
                        isPreview = false
                    },
                    modifier = Modifier.weight(0.38f)
                )
                NotesEditorPane(
                    note = selectedNote,
                    noteImages = state.noteImages,
                    isPreview = isPreview,
                    showBack = false,
                    onBack = {},
                    onTogglePreview = { isPreview = !isPreview },
                    onUpdateNote = onUpdateNote,
                    onPickImage = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveImage = onRemoveImage,
                    onRequestDelete = { showDeleteConfirm = true },
                    modifier = Modifier.weight(0.62f)
                )
            }
        } else {
            if (selectedNote == null) {
                NotesListPane(
                    notes = filteredNotes,
                    selectedNoteId = null,
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onSelectNote = {
                        selectedNoteId = it
                        isPreview = false
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                NotesEditorPane(
                    note = selectedNote,
                    noteImages = state.noteImages,
                    isPreview = isPreview,
                    showBack = true,
                    onBack = {
                        selectedNoteId = null
                        isPreview = false
                    },
                    onTogglePreview = { isPreview = !isPreview },
                    onUpdateNote = onUpdateNote,
                    onPickImage = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveImage = onRemoveImage,
                    onRequestDelete = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NotesListPane(
    notes: List<StudyNote>,
    selectedNoteId: String?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onSelectNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SectionPanel(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search notes…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("notes_search_input")
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (notes.isEmpty()) {
            EmptyStatePanel(
                icon = Icons.Outlined.Description,
                message = "No notes yet. Tap New note to create one."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val selected = note.id == selectedNoteId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { onSelectNote(note.id) }
                            .minimumInteractiveComponentSize()
                            .testTag("note_item_${note.id}"),
                        shape = RoundedCornerShape(11.dp),
                        color = MonostudyPalette.PanelElevated,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = note.title.ifBlank { "Untitled note" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${note.subject} · ${note.body.take(50).replace("\n", " ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesEditorPane(
    note: StudyNote?,
    noteImages: Map<String, String>,
    isPreview: Boolean,
    showBack: Boolean,
    onBack: () -> Unit,
    onTogglePreview: () -> Unit,
    onUpdateNote: (StudyNote, String, String, String) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: (StudyNote, String) -> Unit,
    onRequestDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    SectionPanel(modifier = modifier) {
        if (note == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStatePanel(
                    icon = Icons.Outlined.Description,
                    message = "Select a note or create a new one."
                )
            }
            return@SectionPanel
        }

        var title by remember(note.id) { mutableStateOf(note.title) }
        var subject by remember(note.id) { mutableStateOf(note.subject) }
        var body by remember(note.id) { mutableStateOf(note.body) }

        LaunchedEffect(note.title, note.subject, note.body) {
            if (title != note.title && note.title.isNotEmpty() && title.isEmpty()) title = note.title
            if (subject != note.subject) subject = note.subject
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showBack) {
                        IconButton(
                            onClick = {
                                onUpdateNote(note, title, subject, body)
                                onBack()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to notes")
                        }
                    }
                    Text(
                        text = "PERSONAL NOTE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            onUpdateNote(note, title, subject, body)
                            onTogglePreview()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("note_preview_toggle")
                    ) {
                        Text(if (isPreview) "Edit" else "Preview")
                    }
                    TextButton(
                        onClick = onRequestDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MonostudyPalette.AccentRed),
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("note_delete_button")
                    ) {
                        Text("Delete")
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it.take(120)
                    onUpdateNote(note, title, subject, body)
                },
                placeholder = { Text("Untitled note") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().testTag("note_title_input")
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactDropdownSelector(
                    label = subject,
                    options = StudyConstants.TASK_SUBJECTS.map { it to it },
                    onSelected = {
                        subject = it
                        onUpdateNote(note, title, subject, body)
                    },
                    modifier = Modifier.testTag("note_subject_selector")
                )
                OutlinedButton(
                    onClick = onPickImage,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("note_add_image_button")
                ) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Image")
                }
            }

            if (isPreview) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MonostudyPalette.PanelElevated,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
                ) {
                    MarkdownPreview(
                        markdown = body,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                OutlinedTextField(
                    value = body,
                    onValueChange = {
                        body = it.take(20000)
                        onUpdateNote(note, title, subject, body)
                    },
                    placeholder = { Text("Write a formula, concept or revision note…") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).testTag("note_body_input")
                )
            }

            if (note.imageIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    note.imageIds.forEach { imgId ->
                        val dataUrl = noteImages[imgId]
                        if (dataUrl != null) {
                            NoteImageThumbnail(
                                dataUrl = dataUrl,
                                onRemove = { onRemoveImage(note, imgId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteImageThumbnail(
    dataUrl: String,
    onRemove: () -> Unit
) {
    val bitmap: ImageBitmap? = remember(dataUrl) {
        try {
            val base64 = dataUrl.substringAfter("base64,", "")
            if (base64.isEmpty()) null
            else {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    Box(modifier = Modifier.size(width = 116.dp, height = 86.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxSize()
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Note diagram attachment",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Surface(
            onClick = onRemove,
            shape = CircleShape,
            color = MonostudyPalette.AccentRed,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove image",
                    tint = Color(0xFF1A0509),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkdownPreview(markdown: String, modifier: Modifier = Modifier) {
    val lines = remember(markdown) { markdown.lines() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (markdown.isBlank()) {
            Text(
                text = "Nothing to preview yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            lines.forEach { rawLine ->
                val line = formatMathText(rawLine)
                when {
                    line.startsWith("### ") -> Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    line.startsWith("## ") -> Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    line.startsWith("# ") -> Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                        val content = line.trimStart().drop(2)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.primary)
                            Text(text = buildInlineStyledText(content), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    line.isBlank() -> Spacer(Modifier.height(4.dp))
                    else -> Text(text = buildInlineStyledText(line), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun buildInlineStyledText(input: String) = buildAnnotatedString {
    var i = 0
    while (i < input.length) {
        when {
            input.startsWith("**", i) -> {
                val end = input.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(input.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(input[i]); i++
                }
            }
            input[i] == '`' -> {
                val end = input.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = MonostudyPalette.Bg)) {
                        append(" ${input.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else {
                    append(input[i]); i++
                }
            }
            input[i] == '*' -> {
                val end = input.indexOf('*', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(input.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(input[i]); i++
                }
            }
            else -> {
                append(input[i]); i++
            }
        }
    }
}
