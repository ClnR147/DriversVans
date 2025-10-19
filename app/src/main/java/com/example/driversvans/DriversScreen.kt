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
    onDriverClick: (Driver) -> Unit = {}, // optional callback
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, allDrivers) { searchDrivers(allDrivers, query) }

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
                    DriverCardCompact(
                        driver = driver,
                        onClick = { onDriverClick(driver) }
                    )
                }
            }
        }
    }
}

/* -------------------- Helpers -------------------- */

private fun formatPhoneDots(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 10) {
        "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6)}"
    } else raw.trim()
}

/** Three-line card:
 *  Row 1  Name
 *  Row 2  Van: {Van Number} • {Year} {Make} {Model}
 *  Row 3  Phone: {xxx.xxx.xxxx} (tap to dial)
 */
@Composable
private fun DriverCardCompact(
    driver: Driver,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current

    val dialIntent = remember(driver.phone) {
        if (driver.phone.isNotBlank()) {
            Intent(
                Intent.ACTION_DIAL,
                Uri.parse("tel:${driver.phone.filter { it.isDigit() || it == '+' }}")
            )
        } else null
    }

    val vanNumber = driver.van.ifBlank { "—" }
    val ymm = listOfNotNull(
        driver.vanYear?.toString(),
        driver.vanMake.takeIf { it.isNotBlank() },
        driver.vanModel.takeIf { it.isNotBlank() }
    ).joinToString(" ").trim()

    val row2 = buildString {
        append("Van: ")
        append(vanNumber)
        if (ymm.isNotEmpty()) {
            append(" • ")
            append(ymm)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Row 1 – Name
            Text(
                text = driver.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Row 2 – Van info
            Spacer(Modifier.height(4.dp))
            Text(
                text = row2,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Row 3 – Phone (click-to-dial)
            if (driver.phone.isNotBlank() && dialIntent != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Phone: ${formatPhoneDots(driver.phone)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { ctx.startActivity(dialIntent) }
                )
            }
        }
    }
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
