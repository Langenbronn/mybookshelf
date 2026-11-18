package com.mybookshelf.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mybookshelf.app.data.AgeGroup
import com.mybookshelf.app.data.BookBand

/** A little colored dot for a book band (an empty circle for "before bands", "free reader", "adult"). */
@Composable
fun BandSwatch(band: BookBand, size: Dp = 14.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(band.argb?.let { Color(it) } ?: Color.Transparent)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}

/** Title of each age section in the list. */
private fun AgeGroup.sectionTitle(): String = when (this) {
    AgeGroup.Baby -> "0–3 years"
    AgeGroup.Age4to5 -> "4–5 years · Reception"
    AgeGroup.Age5to7 -> "5–7 years · Year 1–2"
    AgeGroup.Age7to9 -> "7–9 years · Year 3–4"
    AgeGroup.Age9to11 -> "9–11 years · Year 5–6"
    AgeGroup.Teen -> "11–15 years"
    AgeGroup.Adult -> "Adult"
    AgeGroup.Unknown -> "Unknown"
}

/**
 * Button + drop-down list to pick one or more book bands (Lilac, Pink… Dark Red).
 * Bands are grouped by age; tapping an age selects / unselects all its bands.
 * The list stays open so several bands can be ticked; tap outside to close it.
 */
@Composable
fun BookBandFilter(
    selected: Set<BookBand>,
    onChange: (Set<BookBand>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    Box(modifier) {
        OutlinedButton(onClick = { open = true }) {
            if (selected.isEmpty()) {
                Text("All book bands")
            } else {
                // Up to 6 colored dots, then the name or the number of bands
                selected.sortedBy { it.ordinal }.take(6).forEach {
                    BandSwatch(it, size = 12.dp)
                    Spacer(Modifier.width(3.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(if (selected.size == 1) selected.first().fullLabel else "${selected.size} bands")
            }
            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose book bands")
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("All book bands") },
                leadingIcon = { Checkbox(checked = selected.isEmpty(), onCheckedChange = null) },
                onClick = { onChange(emptySet()) },
            )
            HorizontalDivider()

            AgeGroup.entries.forEach { group ->
                val bands = BookBand.forAge(group)
                if (bands.size == 1) {
                    // Only one "band" (before bands, free reader, adult): a single line
                    BandItem(
                        bands.first(), selected, onChange,
                        trailing = if (group == AgeGroup.Adult || group == AgeGroup.Unknown) null else group.label,
                    )
                } else {
                    val count = bands.count { it in selected }
                    val state = when (count) {
                        0 -> ToggleableState.Off
                        bands.size -> ToggleableState.On
                        else -> ToggleableState.Indeterminate
                    }
                    DropdownMenuItem(
                        text = { Text(group.sectionTitle(), style = MaterialTheme.typography.titleSmall) },
                        leadingIcon = { TriStateCheckbox(state = state, onClick = null) },
                        onClick = {
                            onChange(if (state == ToggleableState.On) selected - bands.toSet() else selected + bands)
                        },
                    )
                    bands.forEach { band ->
                        BandItem(band, selected, onChange, trailing = "ORT ${band.ort}", indent = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun BandItem(
    band: BookBand,
    selected: Set<BookBand>,
    onChange: (Set<BookBand>) -> Unit,
    trailing: String?,
    indent: Boolean = false,
) {
    val checked = band in selected
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BandSwatch(band)
                Spacer(Modifier.width(10.dp))
                Text(band.label)
            }
        },
        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
        trailingIcon = trailing?.let {
            {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = { onChange(if (checked) selected - band else selected + band) },
        modifier = if (indent) Modifier.padding(start = 24.dp) else Modifier,
    )
}
