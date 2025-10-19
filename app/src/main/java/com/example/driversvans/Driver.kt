package com.example.driversvans.model

/**
 * Represents a single driver and their assigned van.
 *
 * - `id`: a simple unique identifier (can be hash of name or incremental)
 * - `name`: driver's full name
 * - `van`: short van identifier (e.g. "Van #12")
 * - `vanYear`: optional manufacturing year (nullable for backward compatibility)
 * - `vanMake`: manufacturer (e.g. Ford)
 * - `vanModel`: model name (e.g. Transit 150)
 * - `phone`: contact number
 */
data class Driver(
    val id: Int,
    val name: String,
    val van: String,
    val vanYear: Int? = null,
    val vanMake: String = "",
    val vanModel: String = "",
    val phone: String = "",
    val active: Boolean = true
) {
    /**
     * Returns a clean, human-readable label combining year/make/model and van number.
     * Example: "2020 Ford Transit 150 • Van #12"
     */
    val vanFullLabel: String
        get() = buildString {
            if (vanYear != null) append(vanYear).append(' ')
            if (vanMake.isNotBlank()) append(vanMake).append(' ')
            if (vanModel.isNotBlank()) append(vanModel).append(' ')
            if (van.isNotBlank()) append("• ").append(van)
        }.trim()
}

