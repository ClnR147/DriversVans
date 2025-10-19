// DriverSearch.kt
package com.example.driversvans.search

import com.example.driversvans.model.Driver

fun searchDrivers(drivers: List<Driver>, query: String): List<Driver> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return drivers

    fun String?.containsQ() = this?.lowercase()?.contains(q) == true

    return drivers.filter { d ->
        d.name.containsQ() ||
                d.van.containsQ() ||
                d.vanMake.containsQ() ||
                d.vanModel.containsQ() ||
                (d.vanYear?.toString()?.contains(q) == true)
    }
}
