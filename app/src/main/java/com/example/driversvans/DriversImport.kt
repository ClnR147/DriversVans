package com.example.driversvans.imports

import android.content.Context
import com.example.driversvans.model.Driver
import com.example.driversvans.store.DriversStore
import jxl.Workbook
import java.io.File

object DriversImport {

    fun importFromXls(
        context: Context,
        xlsFile: File,
        merge: Boolean = true,
        sheetName: String = "Drivers"
    ): List<Driver> {
        require(xlsFile.exists()) { "XLS not found: ${xlsFile.absolutePath}" }

        val book = Workbook.getWorkbook(xlsFile)
        val sheet = book.getSheet(sheetName) ?: book.getSheet(0)

        val headerRow = 0
        fun idx(header: String): Int? {
            for (c in 0 until sheet.columns) {
                val contents = sheet.getCell(c, headerRow)?.contents?.trim().orEmpty()
                if (contents.equals(header, ignoreCase = true)) return c
            }
            return null
        }

        val nameCol  = idx("Name") ?: error("Header 'Name' not found")
        val vanCol   = idx("Van") ?: idx("Unit") ?: error("Header 'Van' (or 'Unit') not found")
        val phoneCol = idx("Phone")
        val yearCol  = idx("Year")
        val makeCol  = idx("Make")
        val modelCol = idx("Model")

        val imported = buildList {
            for (r in 1 until sheet.rows) {
                val name = sheet.getCell(nameCol, r)?.contents?.trim().orEmpty()
                if (name.isBlank()) continue

                val van = sheet.getCell(vanCol, r)?.contents?.trim().orEmpty()
                val phone = phoneCol?.let { sheet.getCell(it, r)?.contents?.trim().orEmpty() } ?: ""

                val yearStr = yearCol?.let { sheet.getCell(it, r)?.contents?.trim().orEmpty() } ?: ""
                val year = yearStr.toIntOrNull()
                val make = makeCol?.let { sheet.getCell(it, r)?.contents?.trim().orEmpty() } ?: ""
                val model = modelCol?.let { sheet.getCell(it, r)?.contents?.trim().orEmpty() } ?: ""

                val id = name.lowercase().hashCode()
                add(
                    Driver(
                        id = id,
                        name = name,
                        van = van,
                        vanYear = year,
                        vanMake = make,
                        vanModel = model,
                        phone = phone,
                        active = true
                    )
                )
            }
        }

        book.close()

        return if (merge) {
            val existing = DriversStore.load(context).associateBy { it.id }.toMutableMap()
            imported.forEach { existing[it.id] = it }
            val merged = existing.values.sortedBy { it.name.lowercase() }
            DriversStore.save(context, merged)
            merged
        } else {
            val sorted = imported.sortedBy { it.name.lowercase() }
            DriversStore.save(context, sorted)
            sorted
        }
    }
}
