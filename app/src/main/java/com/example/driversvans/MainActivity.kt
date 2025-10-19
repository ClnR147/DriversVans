package com.example.driversvans

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.* // Column, Row, padding, Arrangement, Spacer, etc.
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
import androidx.lifecycle.lifecycleScope
// ❌ DO NOT import androidx.media3.exoplayer.offline.Download
import com.example.driversvans.model.Driver
import com.example.driversvans.store.DriversStore
import com.example.driversvans.imports.DriversImport
import com.example.driversvans.ui.DriversScreen
import com.example.driversvans.ui.theme.DriverVansTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DriverVansTheme {
                val ctx = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }

                var drivers by remember { mutableStateOf(emptyList<Driver>()) }
                var showAddDialog by remember { mutableStateOf(false) }

                // Load on launch
                LaunchedEffect(Unit) {
                    drivers = DriversStore.load(ctx)
                }

                // Handlers
                fun refresh() {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val fresh = DriversStore.load(ctx)
                        launch(Dispatchers.Main) { drivers = fresh }
                    }
                }

                fun importFromXls() {
                    // Adjust this path to where Syncthing drops your spreadsheet
                    // e.g. C:/AutoSyncToPhone/DriverVans on PC → /storage/emulated/0/DriverVans on phone
                    val xlsPath = "/storage/emulated/0/DriverVans/Drivers.xls"
                    lifecycleScope.launch(Dispatchers.IO) {
                        val f = File(xlsPath)
                        try {
                            val list = DriversImport.importFromXls(ctx, f, merge = true)
                            launch(Dispatchers.Main) {
                                drivers = list
                                snackbarHostState.showSnackbar("Imported ${list.size} drivers from spreadsheet.")
                            }
                        } catch (e: Exception) {
                            launch(Dispatchers.Main) {
                                snackbarHostState.showSnackbar("Import failed: ${e.message ?: "unknown error"}")
                            }
                        }
                    }
                }

                fun addDriver(d: Driver) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val updated = DriversStore.upsert(ctx, d)
                        launch(Dispatchers.Main) {
                            drivers = updated
                            snackbarHostState.showSnackbar("Saved ${d.name}")
                        }
                    }
                }

                // UI
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
                        onImport = ::importFromXls,       // optional; also exposed via the top bar button
                        onAdd = { showAddDialog = true }, // optional; also exposed via FAB
                        onDriverClick = { /* open editor later if you add one */ },
                        modifier = Modifier.padding(padding)
                    )
                }

                if (showAddDialog) {
                    AddDriverDialog(
                        onDismiss = { showAddDialog = false },
                        onSave = { name, van, yearStr, make, model, phone ->
                            val id = name.trim().lowercase().hashCode()
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
                OutlinedTextField(value = van, onValueChange = { van = it }, label = { Text("Van (e.g. Van #12)") }, singleLine = true)
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
                onClick = {
                    if (name.isNotBlank()) onSave(name, van, year, make, model, phone)
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
