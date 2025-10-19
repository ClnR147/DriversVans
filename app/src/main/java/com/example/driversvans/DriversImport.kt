package com.example.driversvans.imports

import android.content.Context
import com.example.driversvans.model.Driver
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileInputStream

object DriversImport {

    fun importFromXls(context: Context, file: File, merge: Boolean = true): List<Driver> {
        if (!file.exists()) throw IllegalArgumentException("File not found: ${file.path}")

        val drivers = mutableListOf<Driver>()

        FileInputStream(file).use { fis ->
            val workbook = HSSFWorkbook(fis)
            val sheet = workbook.getSheetAt(0)

            // Read header row to find columns
            val headerRow = sheet.getRow(0)
            val colMap = mutableMapOf<String, Int>()
            for (cellIndex in 0 until headerRow.physicalNumberOfCells) {
                val name = headerRow.getCell(cellIndex)?.stringCellValue?.trim()?.lowercase() ?: continue
                colMap[name] = cellIndex
            }

            // Locate expected columns
            val nameCol = colMap.entries.find { it.key.contains("name") }?.value
            val vanCol = colMap.entries.find { it.key.contains("van") }?.value
            val yearCol = colMap.entries.find { it.key.contains("year") }?.value
            val makeCol = colMap.entries.find { it.key.contains("make") }?.value
            val modelCol = colMap.entries.find { it.key.contains("model") }?.value
            val phoneCol = colMap.entries.find { it.key.contains("phone") }?.value

            if (nameCol == null) throw IllegalArgumentException("No 'Name' column found.")
            if (vanCol == null) throw IllegalArgumentException("No 'Van' column found.")

            // Loop through rows
            for (r in 1..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue

                val name = row.getCell(nameCol)?.toString()?.trim().orEmpty()
                if (name.isBlank()) continue

                val van = row.getCell(vanCol)?.toString()?.trim().orEmpty()
                val yearStr = yearCol?.let { row.getCell(it)?.toString()?.trim().orEmpty() }.orEmpty()
                val make = makeCol?.let { row.getCell(it)?.toString()?.trim().orEmpty() }.orEmpty()
                val model = modelCol?.let { row.getCell(it)?.toString()?.trim().orEmpty() }.orEmpty()
                val phone = phoneCol?.let { row.getCell(it)?.toString()?.trim().orEmpty() }.orEmpty()

                val year = yearStr.toDoubleOrNull()?.toInt()
                val id = (name.lowercase() + "|" + phone).hashCode()

                drivers += Driver(
                    id = id,
                    name = name,
                    van = van,
                    vanYear = year,
                    vanMake = make,
                    vanModel = model,
                    phone = phone
                )
            }

            workbook.close()
        }

        // ✅ Sort numerically by van number (if numeric)
        return drivers.sortedWith(compareBy { it.van.toIntOrNull() ?: Int.MAX_VALUE })
    }
}
