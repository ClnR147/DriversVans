package com.example.driversvans.imports

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.driversvans.model.Driver
import com.example.driversvans.storage.TreeAccess
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

object DriversImport {

    /** Import from the previously granted folder (persisted with TreeAccess). */
    fun importFromPersistedFolder(ctx: Context, fileName: String = "Drivers.xls"): List<Driver> {
        val treeUri = TreeAccess.getTreeUri(ctx) ?: error("Folder not granted yet.")
        val tree = DocumentFile.fromTreeUri(ctx, treeUri) ?: error("Saved folder unavailable.")
        val target = tree.listFiles().firstOrNull { it.name.equals(fileName, ignoreCase = true) }
            ?: error("$fileName not found in granted folder.")

        // Try XLS first
        ctx.contentResolver.openInputStream(target.uri)?.use { ins ->
            runCatching {
                HSSFWorkbook(ins).use { wb -> return parseSheet(wb.getSheetAt(0)) }
            }
        }
        // Then XLSX
        ctx.contentResolver.openInputStream(target.uri)?.use { ins ->
            runCatching {
                XSSFWorkbook(ins).use { wb -> return parseSheet(wb.getSheetAt(0)) }
            }.getOrThrow()
        }

        error("Unable to read $fileName (neither .xls nor .xlsx).")
    }

    /** Optional: direct URI import if you ever need it. */
    fun importFromUri(ctx: Context, uri: Uri): List<Driver> {
        ctx.contentResolver.openInputStream(uri)?.use { ins ->
            runCatching {
                HSSFWorkbook(ins).use { wb -> return parseSheet(wb.getSheetAt(0)) }
            }
        }
        ctx.contentResolver.openInputStream(uri)?.use { ins ->
            runCatching {
                XSSFWorkbook(ins).use { wb -> return parseSheet(wb.getSheetAt(0)) }
            }.getOrThrow()
        }
        error("Unable to read spreadsheet at $uri")
    }

    /** Map the sheet to Driver list. Only reads: Name, Van, Year, Make, Model, Phone. */
    private fun parseSheet(sheet: Sheet): List<Driver> {
        val header = sheet.getRow(0) ?: return emptyList()

        val cols = buildMap<String, Int> {
            for (i in 0 until header.physicalNumberOfCells) {
                val key = header.getCell(i)?.toString()?.trim()?.lowercase() ?: continue
                put(key, i)
            }
        }
        val nameCol  = cols.entries.find { it.key.contains("name") }?.value
        val vanCol   = cols.entries.find { it.key.contains("van") }?.value
        val yearCol  = cols.entries.find { it.key.contains("year") }?.value
        val makeCol  = cols.entries.find { it.key.contains("make") }?.value
        val modelCol = cols.entries.find { it.key.contains("model") }?.value
        val phoneCol = cols.entries.find { it.key.contains("phone") }?.value

        require(nameCol != null) { "No 'Name' column found." }
        require(vanCol  != null) { "No 'Van' column found." }

        val drivers = mutableListOf<Driver>()
        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue

            val name = row.getCell(nameCol)?.toString()?.trim().orEmpty()
            if (name.isBlank()) continue

            val van    = row.getCell(vanCol)?.toString()?.trim().orEmpty()   // number-only per your sheet
            val year   = yearCol?.let { row.getCell(it)?.toString()?.toDoubleOrNull()?.toInt() }
            val make   = makeCol?.let { row.getCell(it)?.toString()?.trim().orEmpty() }.orEmpty()
            val model  = modelCol?.let { row.getCell(it)?.toString()?.trim().orEmpty() }.orEmpty()
            val phone  = phoneCol?.let { row.getCell(it)?.toString()?.trim().orEmpty() }.orEmpty()

            val id = (name.lowercase() + "|" + phone).hashCode()
            drivers += Driver(
                id = kotlin.math.abs(id),
                name = name,
                van = van,
                vanYear = year,
                vanMake = make,
                vanModel = model,
                phone = phone
            )
        }

        // Sort numerically by van (non-numerics last, just in case)
        return drivers.sortedWith(compareBy { it.van.toIntOrNull() ?: Int.MAX_VALUE })
    }
}
