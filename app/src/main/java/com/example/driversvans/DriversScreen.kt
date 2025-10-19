package com.example.driversvans.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.driversvans.model.Driver
import com.example.driversvans.search.searchDrivers

@Composable
fun DriversScreen(
    allDrivers: List<Driver>,
    onRefresh: () -> Unit = {},
    onImport: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    onDriverClick: (Driver) -> Unit = {}, // still available if you want to hook edit later
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, allDrivers) { searchDrivers(allDrivers, query) }

    // Selected driver for the detail dialog
    var selected by remember { mutableStateOf<Driver?>(null) }

    Column(modifier.fillMaxSize()) {

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            label = { Text("Search by van or name") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )

        // Optional actions
        if (onImport != null || onAdd != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onImport != null) {
                    OutlinedButton(onClick = onImport) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import from Spreadsheet")
                    }
                }
                if (onAdd != null) {
                    Button(onClick = onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Driver")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // List
        if (results.isEmpty()) {
            EmptyState(
                hasQuery = query.isNotBlank(),
                onClearQuery = { query = "" }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(results, key = { it.id }) { driver ->
                    DriverRowCompact(
                        driver = driver,
                        onClick = {
                            selected = driver          // show details on tap
                            onDriverClick(driver)      // still callbacks if you use it elsewhere
                        }
                    )
                }
            }
        }
    }

    // Details dialog
    selected?.let { d ->
        DriverDetailsDialog(
            driver = d,
            onDismiss = { selected = null }
        )
    }
}

/** Compact single-line row: [PILL VAN]  Name  …  Phone (clickable) */
@Composable
private fun DriverRowCompact(
    driver: Driver,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current

    // Prebuild dial intent
    val dialIntent = remember(driver.phone) {
        if (driver.phone.isNotBlank()) {
            Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:${driver.phone.filter { ch -> ch.isDigit() || ch == '+' }}")
            )
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Van pill
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = driver.van.ifBlank { "Van" },
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Name (expand)
            Text(
                text = driver.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Right-aligned clickable phone number (no “Call” text)
            if (driver.phone.isNotBlank() && dialIntent != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = driver.phone,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.clickable { ctx.startActivity(dialIntent) }
                )
            }
        }
    }
}

/** Simple details dialog with full driver info and tap-to-dial number. */
@Composable
private fun DriverDetailsDialog(
    driver: Driver,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    val dialIntent = remember(driver.phone) {
        if (driver.phone.isNotBlank()) {
            Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:${driver.phone.filter { ch -> ch.isDigit() || ch == '+' }}")
            )
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(driver.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Full van info
                val vanFull = driver.vanFullLabel.ifBlank { driver.van }
                if (vanFull.isNotBlank()) Text("Van: $vanFull")
                // Individual fields (helpful if some are missing in full label)
                if (driver.vanYear != null || driver.vanMake.isNotBlank() || driver.vanModel.isNotBlank()) {
                    val year = driver.vanYear?.toString().orEmpty()
                    val make = driver.vanMake
                    val model = driver.vanModel
                    Text("Details: " + listOf(year, make, model).filter { it.isNotBlank() }.joinToString(" "))
                }
                if (driver.van.isNotBlank()) Text("Label: ${driver.van}")

                if (driver.phone.isNotBlank()) {
                    TextButton(
                        onClick = { dialIntent?.let(ctx::startActivity) }
                    ) {
                        Text(driver.phone) // clickable number, no “Call” text
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

/** Empty/search state */
@Composable
private fun EmptyState(
    hasQuery: Boolean,
    onClearQuery: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (hasQuery) "No drivers match your search." else "No drivers yet.",
                style = MaterialTheme.typography.bodyLarge
            )
            if (hasQuery) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClearQuery) { Text("Clear search") }
            }
        }
    }
}
