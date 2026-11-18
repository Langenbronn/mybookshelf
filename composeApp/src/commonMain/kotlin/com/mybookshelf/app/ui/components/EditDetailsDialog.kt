package com.mybookshelf.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.AgeGroup
import com.mybookshelf.app.data.BookBand
import com.mybookshelf.app.data.BookClassifier
import com.mybookshelf.app.data.BookInfo
import com.mybookshelf.app.data.BookKind
import com.mybookshelf.app.data.Corrections
import com.mybookshelf.app.data.ReadingLevels

/**
 * Lets the user correct the details the app guessed: type, format, genres, themes, reading age, book band.
 * Each field has "Automatic" (= let the app guess) as first choice.
 *
 * @param onSave called with the new corrections (null = everything automatic).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDetailsDialog(book: BookInfo, onSave: (Corrections?) -> Unit, onDismiss: () -> Unit) {
    val start = book.corrections ?: Corrections()
    // The book without corrections: what the app guesses on its own (shown as "Automatic (…)")
    val auto = book.copy(corrections = null)
    val autoKind = BookClassifier.kind(auto)

    var kind by remember { mutableStateOf(start.kind) }
    var format by remember { mutableStateOf(start.format) }
    var genres by remember { mutableStateOf(start.genres) }
    var themes by remember { mutableStateOf(start.themes) }
    var ageGroup by remember { mutableStateOf(start.ageGroup) }
    var band by remember { mutableStateOf(start.band) }

    // Genres list depends on fiction / non-fiction
    val effectiveKind = kind ?: autoKind
    val genreChoices = if (effectiveKind == BookKind.NonFiction) {
        BookClassifier.schoolSubjectNames
    } else {
        BookClassifier.fictionGenreNames
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(book.title, style = MaterialTheme.typography.titleSmall)

                // ---- Type ----
                Section("Type")
                Choice(
                    selected = kind,
                    options = BookKind.entries,
                    label = { it.label },
                    automatic = "Automatic (${autoKind?.label ?: "unknown"})",
                    onSelect = { kind = it },
                )

                // ---- Format ----
                Section("Format")
                Choice(
                    selected = format,
                    options = BookClassifier.formatNames,
                    label = { it },
                    automatic = "Automatic (${BookClassifier.format(auto, effectiveKind) ?: "unknown"})",
                    onSelect = { format = it },
                )

                // ---- Genres / Subjects ----
                Section(if (effectiveKind == BookKind.NonFiction) "Subjects" else "Genres")
                MultiChoice(
                    selected = genres,
                    options = genreChoices,
                    automaticHint = BookClassifier.genres(auto, effectiveKind).joinToString(", ").ifEmpty { "unknown" },
                    onChange = { genres = it },
                )

                // ---- Themes ----
                Section("Themes")
                MultiChoice(
                    selected = themes,
                    options = BookClassifier.themeNames,
                    automaticHint = BookClassifier.themes(auto).joinToString(", ").ifEmpty { "unknown" },
                    onChange = { themes = it },
                )

                // ---- Reading age & book band ----
                Section("Reading age")
                val autoLevel = ReadingLevels.of(auto)
                Choice(
                    selected = ageGroup,
                    options = AgeGroup.entries.filter { it != AgeGroup.Unknown },
                    label = { if (it == AgeGroup.Adult) "Adult" else "${it.label} years" },
                    automatic = "Automatic (${readingAgeText(autoLevel)})",
                    onSelect = {
                        ageGroup = it
                        if (band?.ageGroup != it) band = null
                    },
                )
                // A precise band only makes sense when the age has several bands (4–5, 5–7, 7–9, 9–11)
                val group = ageGroup
                val bands = group?.let { BookBand.forAge(it) }.orEmpty()
                if (bands.size > 1) {
                    Section("Book band")
                    Choice(
                        selected = band,
                        options = bands,
                        label = { it.fullLabel },
                        automatic = "Any band for ${group?.label} years",
                        leading = { BandSwatch(it) },
                        onSelect = { band = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(Corrections(kind, format, genres, themes, ageGroup, band))
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(null) }) { Text("All automatic") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun Section(title: String) {
    HorizontalDivider()
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** A button showing the current choice; tapping it opens a list. null = "Automatic". */
@Composable
private fun <T> Choice(
    selected: T?,
    options: List<T>,
    label: (T) -> String,
    automatic: String,
    onSelect: (T?) -> Unit,
    leading: (@Composable (T) -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (selected != null && leading != null) {
                    leading(selected)
                    Spacer(Modifier.width(8.dp))
                }
                Text(selected?.let(label) ?: automatic)
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(automatic, style = MaterialTheme.typography.bodyMedium) },
                onClick = { onSelect(null); open = false },
            )
            HorizontalDivider()
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    leadingIcon = leading?.let { { it(option) } },
                    onClick = { onSelect(option); open = false },
                )
            }
        }
    }
}

/** Chips to tick several values, plus an "Automatic" chip. null = automatic. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoice(
    selected: List<String>?,
    options: List<String>,
    automaticHint: String,
    onChange: (List<String>?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onChange(null) },
            label = { Text("Automatic ($automaticHint)") },
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                val checked = selected?.contains(option) == true
                FilterChip(
                    selected = checked,
                    onClick = {
                        val current = selected.orEmpty()
                        val next = if (checked) current - option else current + option
                        // Nothing ticked anymore = back to automatic
                        onChange(next.ifEmpty { null })
                    },
                    label = { Text(option) },
                )
            }
        }
    }
}
