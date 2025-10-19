package com.example.driversvans

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.driversvans.model.Driver
import com.example.driversvans.store.DriversStore
import com.example.driversvans.imports.DriversImport
import com.example.driversvans.storage.TreeAccess
import com.example.driversvans.ui.DriversScreen
import com.example.driversvans.ui.theme.DriverVansTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DriverVansTheme {
                val ctx = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                var drivers by remember { mutableStateOf(emptyList<Driver>()) }
                var showAddDialog by remember { mutableStateOf(false) }

                // ---------- Helpers (single definitions) ----------
                fun phoneDigits(s: String) = s.filter { it.isDigit() }

                fun mergeDrivers(existing: List<Driver>, imported: List<Driver>): List<Driver> {
                    // Key primarily by (name|phoneDigits); fallback to (name|van)
                    fun keyOf(d: Driver): String {
                        val phoneKey = phoneDigits(d.phone)
                        return if (phoneKey.isNotEmpty())
                            d.name.trim().lowercase() + "|" + phoneKey
                        else
                            d.name.trim().lowercase() + "|van:" + d.van.trim().lowercase()
                    }

                    val byKey = existing.associateBy(::keyOf).toMutableMap()

                    for (imp in imported) {
                        val k = keyOf(imp)
                        val prev = byKey[k]
                        if (prev == null) {
                            val newId = abs((imp.name.trim().lowercase() + "|" + phoneDigits(imp.phone)).hashCode())
                            byKey[k] = imp.copy(id = newId)
                        } else {
                            byKey[k] = prev.copy(
                                name    = imp.name.ifBlank { prev.name },
                                van     = imp.van.ifBlank { prev.van },
                                vanYear = imp.vanYear ?: prev.vanYear,
                                vanMake = if (imp.vanMake.isNotBlank()) imp.vanMake else prev.vanMake,
                                vanModel= if (imp.vanModel.isNotBlank()) imp.vanModel else prev.vanModel,
                                phone   = if (imp.phone.isNotBlank()) imp.phone else prev.phone
                            )
                        }
                    }

                    return byKey.values.sortedWith(compareBy { it.van.toIntOrNull() ?: Int.MAX_VALUE })
                }

                // ---------- One-time folder grant launcher ----------
                val grantFolderLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    if (uri != null) {
                        TreeAccess.saveTreeUri(ctx, uri)
                        // Immediately attempt first import after grant
                        scope.launch {
                            try {
                                val imported = withContext(Dispatchers.IO) {
                                    DriversImport.importFromPersistedFolder(ctx, fileName = "Drivers.xls")
                                }
                                val existing = withContext(Dispatchers.IO) { DriversStore.load(ctx) }
                                val merged = mergeDrivers(existing, imported)

                                // Persist merged set using upsert loop (works without replaceAll)
                                withContext(Dispatchers.IO) {
                                    var acc = existing
                                    for (d in merged) {
                                        acc = DriversStore.upsert(ctx, d)
                                    }
                                }

                                val fresh = withContext(Dispatchers.IO) { DriversStore.load(ctx) }
                                drivers = fresh.sortedWith(compareBy { it.van.toIntOrNull() ?: Int.MAX_VALUE })

                                snackbarHostState.showSnackbar("Imported ${imported.size} rows. ${fresh.size} total after merge.")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Import failed: ${e.message ?: "unknown error"}")
                            }
                        }
                    }
                }

                // Load on launch
                LaunchedEffect(Unit) {
                    drivers = withContext(Dispatchers.IO) { DriversStore.load(ctx) }
                }

                // ---------- Handlers ----------
                fun refresh() {
                    scope.launch {
                        val fresh = withContext(Dispatchers.IO) { DriversStore.load(ctx) }
                        drivers = fresh
                        snackbarHostState.showSnackbar("Refreshed ${fresh.size} drivers.")
                    }
                }

                fun importFromXls() {
                    scope.launch {
                        if (TreeAccess.getTreeUri(ctx) == null) {
                            snackbarHostState.showSnackbar("Pick the DriverVans folder once to allow imports.")
                            grantFolderLauncher.launch(null)
                            return@launch
                        }

                        try {
                            val imported = withContext(Dispatchers.IO) {
                                DriversImport.importFromPersistedFolder(ctx, fileName = "Drivers.xls")
                            }
                            val existing = withContext(Dispatchers.IO) { DriversStore.load(ctx) }
                            val merged = mergeDrivers(existing, imported)

                            // ✅ Persist the WHOLE merged list in one go (no overwrites, no partials)
                            withContext(Dispatchers.IO) {
                                DriversStore.replaceAll(ctx, merged)
                            }

                            // Reload for UI (already sorted by replaceAll)
                            val fresh = withContext(Dispatchers.IO) { DriversStore.load(ctx) }
                            drivers = fresh

                            snackbarHostState.showSnackbar(
                                "Imported ${imported.size} rows. ${fresh.size} total after merge."
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Import failed: ${e.message ?: "unknown error"}")
                        }
                    }
                }


                fun addDriver(d: Driver) {
                    scope.launch {
                        val updated = withContext(Dispatchers.IO) { DriversStore.upsert(ctx, d) }
                        drivers = updated
                        snackbarHostState.showSnackbar("Saved ${d.name}")
                    }
                }

                // ---------- UI ----------
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Driver Vans") },
                            actions = {
                                IconButton(onClick = { importFromXls() }) {
                                    Icon(Icons.Filled.Download, contentDescription = "Import from spreadsheet")
                                }
                                IconButton(onClick = { refresh() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Driver")
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { padding ->
                    DriversScreen(
                        allDrivers = drivers,
                        onRefresh = ::refresh,
                        onImport = ::importFromXls,
                        onAdd = { showAddDialog = true },
                        onDriverClick = { /* open editor later if you add one */ },
                        modifier = Modifier.padding(padding)
                    )
                }

                if (showAddDialog) {
                    AddDriverDialog(
                        onDismiss = { showAddDialog = false },
                        onSave = { name, van, yearStr, make, model, phone ->
                            val id = abs((name.trim().lowercase() + "|" + phone.trim()).hashCode())
                            val year = yearStr.toIntOrNull()
                            val d = Driver(
                                id = id,
                                name = name.trim(),
                                van = van.trim(),
                                vanYear = year,
                                vanMake = make.trim(),
                                vanModel = model.trim(),
                                phone = phone.trim()
                            )
                            addDriver(d)
                            showAddDialog = false
                        }
                    )
                }
            }
        }
    }
}

/* -------------------- Add Driver Dialog -------------------- */

@Composable
private fun AddDriverDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, van: String, year: String, make: String, model: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var van by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Driver") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Driver Name") }, singleLine = true)
                OutlinedTextField(value = van, onValueChange = { van = it }, label = { Text("Van (number only)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year, onValueChange = { year = it },
                        label = { Text("Year") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = make, onValueChange = { make = it },
                        label = { Text("Make") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, singleLine = true)
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Phone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name, van, year, make, model, phone) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
