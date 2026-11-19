package com.mybookshelf.app.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import com.mybookshelf.app.ui.components.countText

/** "3 books" (or "2 of 3 books" when searching), the Compact / Detailed switch and the ⋮ menu. */
@Composable
internal fun LibraryHeader(
    shown: Int,
    total: Int,
    compact: Boolean,
    onCompactChange: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onExportAll: () -> Unit,
    onImport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (shown != total) "$shown of ${countText(total, "book")}" else countText(total, "book"),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = compact,
                onClick = { onCompactChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Compact") }
            SegmentedButton(
                selected = !compact,
                onClick = { onCompactChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Detailed") }
        }
        LibraryMenu(onSelect, onExportAll, onImport)
    }
}

/** The ⋮ button and its menu: select books to export, export all, import a file. */
@Composable
private fun LibraryMenu(onSelect: () -> Unit, onExportAll: () -> Unit, onImport: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    // The Box keeps the menu next to its button
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Select books to export") },
                leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                onClick = { open = false; onSelect() },
            )
            DropdownMenuItem(
                text = { Text("Export all books") },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = { open = false; onExportAll() },
            )
            DropdownMenuItem(
                text = { Text("Import books (CSV file)") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = { open = false; onImport() },
            )
        }
    }
}

/**
 * Shown while selecting books: how many are ticked, "All" / "None", "Export" and a close button.
 * @param onTickAll true = tick every book shown, false = untick them.
 */
@Composable
internal fun SelectionBar(
    count: Int,
    allShownTicked: Boolean,
    onTickAll: (Boolean) -> Unit,
    onExport: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Stop selecting") }
            Text("$count selected", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { onTickAll(!allShownTicked) }) {
                Text(if (allShownTicked) "None" else "All")
            }
            Button(onClick = onExport, enabled = count > 0) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export")
            }
        }
    }
}
