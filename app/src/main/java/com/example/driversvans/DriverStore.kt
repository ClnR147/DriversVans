// DriversStore.kt
package com.example.driversvans.store

import android.content.Context
import com.example.driversvans.model.Driver
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

@Serializable
private data class DriverDTO(
    val id: Int,
    val name: String,
    val van: String,
    val phone: String,

    // NEW (defaults keep old JSON readable)
    val vanYear: Int? = null,
    val vanMake: String = "",
    val vanModel: String = "",
    val active: Boolean = true
)

private fun DriverDTO.toModel() = Driver(
    id = id,
    name = name,
    van = van,
    vanYear = vanYear,
    vanMake = vanMake,
    vanModel = vanModel,
    phone = phone,
    active = active
)

private fun Driver.toDto() = DriverDTO(
    id = id,
    name = name,
    van = van,
    phone = phone,
    vanYear = vanYear,
    vanMake = vanMake,
    vanModel = vanModel,
    active = active
)

object DriversStore {
    private const val FILE_NAME = "drivers.json"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun sortDrivers(list: List<Driver>): List<Driver> =
        list.sortedWith(
            compareBy<Driver> { it.van.toIntOrNull() ?: Int.MAX_VALUE }
                .thenBy { it.name.lowercase() }
        )

    fun load(context: Context): List<Driver> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        val dtos = json.decodeFromString(ListSerializer(DriverDTO.serializer()), f.readText())
        return dtos.map { it.toModel() }
    }

    fun save(context: Context, drivers: List<Driver>) {
        val dtos = drivers.map { it.toDto() }
        file(context).writeText(json.encodeToString(ListSerializer(DriverDTO.serializer()), dtos))
    }

    /** 🔹 Bulk replace the entire store with the provided list (after sorting). */
    fun replaceAll(context: Context, drivers: List<Driver>): List<Driver> {
        val sorted = sortDrivers(drivers)
        save(context, sorted)
        return sorted
    }

    /** Upsert a single driver; returns the updated, sorted list. */
    fun upsert(context: Context, driver: Driver): List<Driver> {
        val list = load(context).toMutableList()
        val idx = list.indexOfFirst { it.id == driver.id }
        if (idx >= 0) list[idx] = driver else list.add(driver)
        val sorted = sortDrivers(list)
        save(context, sorted)
        return sorted
    }
    // Add to DriversStore object
    fun delete(context: Context, id: Int): List<Driver> {
        val updated = load(context).filterNot { it.id == id }
        save(context, updated)
        return updated
    }

    fun deleteMany(context: Context, ids: Collection<Int>): List<Driver> {
        val set = ids.toSet()
        val updated = load(context).filterNot { it.id in set }
        save(context, updated)
        return updated
    }

    fun setActive(context: Context, id: Int, active: Boolean): List<Driver> {
        val updated = load(context).map { if (it.id == id) it.copy(active = active) else it }
        val sorted = sortDrivers(updated)
        save(context, sorted)
        return sorted
    }
}
