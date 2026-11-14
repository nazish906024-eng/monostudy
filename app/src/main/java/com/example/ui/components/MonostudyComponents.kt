package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MonostudyPalette

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.size(8.dp))
                trailing()
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionPanel(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun PanelHeader(
    eyebrow: String? = null,
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!eyebrow.isNullOrBlank()) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun StatMetricCard(
    label: String,
    value: String,
    caption: String,
    progressFraction: Float? = null,
    accentColor: Color = MonostudyPalette.Primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (progressFraction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                StudyProgressBar(fraction = progressFraction, color = accentColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudyProgressBar(
    fraction: Float,
    color: Color = MonostudyPalette.Primary,
    modifier: Modifier = Modifier
) {
    val clamped = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(MonostudyPalette.PanelElevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(color)
        )
    }
}

@Composable
fun StudyChip(
    text: String,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    backgroundColor: Color = MonostudyPalette.PanelElevated,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(99.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun WeightageChip(
    band: String,
    labelPrefix: String = ""
) {
    val color = when (band) {
        "High" -> MonostudyPalette.AccentRed
        "Medium" -> MonostudyPalette.AccentOrange
        "Low" -> MonostudyPalette.AccentGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    StudyChip(
        text = if (labelPrefix.isEmpty()) band else "$band $labelPrefix",
        borderColor = color.copy(alpha = 0.7f),
        textColor = color
    )
}

@Composable
fun SelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(99.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MonostudyPalette.PanelElevated,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun EmptyStatePanel(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Formats inline LaTeX math snippets (such as $v=u+at$, $\tfrac12$, $\Omega$, ^2)
 * into clean Unicode mathematical notation for native Compose rendering.
 */
fun formatMathText(raw: String): String {
    if (!raw.contains("$") && !raw.contains("\\")) return raw
    var s = raw
    val replacements = listOf(
        "\\tfrac12" to "½",
        "\\tfrac{1}{2}" to "½",
        "\\dfrac{u^2}{2g}" to "(u² / 2g)",
        "\\dfrac{u^2\\sin2\\theta}{g}" to "(u² sin 2θ) / g",
        "\\dfrac{0.693}{k}" to "(0.693 / k)",
        "\\dfrac{0.0591}{n}" to "(0.0591 / n)",
        "\\dfrac{-b\\pm\\sqrt{b^2-4ac}}{2a}" to "(-b ± √(b² - 4ac)) / 2a",
        "\\dfrac{d\\Phi_B}{dt}" to "dΦ_B/dt",
        "\\dfrac{1}{R}" to "1/R",
        "\\dfrac1R" to "1/R",
        "\\dfrac14" to "1/4",
        "\\dfrac12" to "1/2",
        "\\dfrac1{R_{eq}}" to "1/R_eq",
        "\\dfrac{1}{R_1}" to "1/R₁",
        "\\dfrac{1}{R_2}" to "1/R₂",
        "\\text{m/s}^2" to "m/s²",
        "\\text{m/s}" to "m/s",
        "\\text{s}^{-1}" to "s⁻¹",
        "\\text{kg}" to "kg",
        "\\text{Fe}^{3+}" to "Fe³⁺",
        "\\text{Ar}" to "Ar",
        "\\text{N}" to "N",
        "\\text{J}" to "J",
        "\\text{m}" to "m",
        "\\text{s}" to "s",
        "\\varepsilon_0" to "ε₀",
        "\\varepsilon" to "ε",
        "\\Phi_B" to "Φ_B",
        "\\Phi" to "Φ",
        "\\Omega" to "Ω",
        "\\theta" to "θ",
        "\\Delta" to "Δ",
        "\\Rightarrow" to "⇒",
        "\\cdot" to "·",
        "\\cdots" to "…",
        "\\times" to "×",
        "\\pm" to "±",
        "\\sin" to "sin",
        "\\cos" to "cos",
        "\\log" to "log",
        "\\sqrt" to "√",
        "\\circ" to "°",
        "\\;" to " ",
        "\\," to " ",
        "^\\circ" to "°",
        "^{2}" to "²",
        "^2" to "²",
        "^{3}" to "³",
        "^3" to "³",
        "^{23}" to "²³",
        "^{20}" to "²⁰",
        "^{3+}" to "³⁺",
        "\\text{" to "",
        "$$" to "",
        "$" to ""
    )
    for ((k, v) in replacements) {
        s = s.replace(k, v)
    }
    s = s.replace(Regex("\\\\dfrac\\{([^}]*)\\}\\{([^}]*)\\}"), "($1)/($2)")
    s = s.replace(Regex("\\\\frac\\{([^}]*)\\}\\{([^}]*)\\}"), "($1)/($2)")
    s = s.replace("{", "").replace("}", "").replace("\\", "")
    return s.trim()
}
