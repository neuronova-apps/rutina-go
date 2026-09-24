package com.neuronovaapps.rutinago.ui.navigation

enum class AppDestination(
    val title: String,
    val label: String = title,
    val symbol: String = "",
    val isMain: Boolean = false,
) {
    HOME("RutinaGo", "Hoy", "⌂", true),
    ROUTINES("Rutinas", "Rutinas", "☰", true),
    CREATE("Crear", "Crear", "+", true),
    PROGRESS("Progreso", "Progreso", "▥", true),
    SETTINGS("Configuración", "Configuración", "⚙", true),
    NOW_NEXT("Ahora / Después"),
    ROUTINE_DETAIL("Detalle de rutina"),
    EXECUTION("Ejecución"),
}

val mainDestinations = listOf(
    AppDestination.HOME,
    AppDestination.ROUTINES,
    AppDestination.CREATE,
    AppDestination.PROGRESS,
    AppDestination.SETTINGS,
)
