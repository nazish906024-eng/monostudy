package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.BundledFlashcards
import com.example.data.model.StudyConstants
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PageHeader
import com.example.ui.components.formatMathText
import com.example.ui.theme.MonostudyPalette
import com.example.ui.viewmodel.StudyDashboardUiState
import kotlin.math.roundToInt

@Composable
fun FlashcardsScreen(
    state: StudyDashboardUiState,
    onMarkCard: (cardId: String, status: String) -> Unit
) {
    var selectedSubject by remember { mutableStateOf("") }
    var cardIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val deck = remember(selectedSubject) {
        BundledFlashcards.deck.filter { selectedSubject.isEmpty() || it.subject == selectedSubject }
    }
    val safeIndex = if (deck.isEmpty()) 0 else cardIndex % deck.size
    val currentCard = deck.getOrNull(safeIndex)
    val knownCount = remember(deck, state.cardProgress) {
        deck.count { state.cardProgress[it.id] == "known" }
    }

    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(),
        label = "card_flip"
    )

    fun markCurrent(status: String) {
        val card = currentCard ?: return
        onMarkCard(card.id, status)
        cardIndex = (safeIndex + 1) % deck.size
        isFlipped = false
        dragOffsetX = 0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("flashcards_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader(
            eyebrow = "Quick revision",
            title = "Flashcards",
            subtitle = "Tap to flip. Swipe right = knew it, swipe left = review again.",
            trailing = {
                CompactDropdownSelector(
                    label = selectedSubject.ifEmpty { "All subjects" },
                    options = listOf("All subjects" to "") + StudyConstants.SUBJECTS.map { it to it },
                    onSelected = {
                        selectedSubject = it
                        cardIndex = 0
                        isFlipped = false
                    },
                    modifier = Modifier.testTag("flashcards_subject_selector")
                )
            }
        )

        if (currentCard == null) {
            EmptyStatePanel(
                icon = Icons.Outlined.Style,
                message = "No flashcards available for this filter."
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Card ${safeIndex + 1} of ${deck.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "· $knownCount known",
                    style = MaterialTheme.typography.bodySmall,
                    color = MonostudyPalette.AccentGreen
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
                    .offset { IntOffset(dragOffsetX.roundToInt(), 0) }
                    .graphicsLayer {
                        this.rotationY = rotationY
                        this.rotationZ = dragOffsetX / 35f
                        cameraDistance = 12f * density
                    }
                    .pointerInput(currentCard.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    dragOffsetX > 140f -> markCurrent("known")
                                    dragOffsetX < -140f -> markCurrent("review")
                                    else -> dragOffsetX = 0f
                                }
                            },
                            onDragCancel = { dragOffsetX = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                dragOffsetX += dragAmount
                            }
                        )
                    }
                    .clickable { isFlipped = !isFlipped }
                    .testTag("flashcard_card"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(MonostudyPalette.Primary.copy(alpha = 0.6f), MonostudyPalette.Line)
                    )
                ),
                tonalElevation = 4.dp
            ) {
                val showBack = rotationY > 90f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .graphicsLayer {
                            if (showBack) this.rotationY = 180f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (showBack) "ANSWER" else currentCard.subject.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (showBack) formatMathText(currentCard.back) else currentCard.front,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedButton(
                    onClick = { markCurrent("review") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("flashcard_review_btn")
                ) {
                    Text("↺ Review again")
                }
                Button(
                    onClick = { markCurrent("known") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("flashcard_known_btn")
                ) {
                    Text("✓ Knew it")
                }
            }

            Text(
                text = "Cards are bundled with the app and synced to your account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
