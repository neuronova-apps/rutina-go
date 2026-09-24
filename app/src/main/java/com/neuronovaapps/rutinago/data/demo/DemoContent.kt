package com.neuronovaapps.rutinago.data.demo

import com.neuronovaapps.rutinago.domain.model.ActivityStep
import com.neuronovaapps.rutinago.domain.model.ProgressRecord
import com.neuronovaapps.rutinago.domain.model.Routine
import com.neuronovaapps.rutinago.domain.model.RoutineCategory
import com.neuronovaapps.rutinago.domain.model.RoutineOutcome

object DemoContent {
    val morningSteps = listOf(
        ActivityStep("wake", "Despertarse", "Siéntate con calma y respira.", "SOL"),
        ActivityStep("bathroom", "Ir al baño", "Ve al baño cuando estés listo.", "BAÑO"),
        ActivityStep("hands", "Lavarse las manos", "Usa agua y jabón.", "MANOS"),
        ActivityStep("clothes", "Elegir la ropa", "Elige una opción cómoda.", "ROPA"),
        ActivityStep("breakfast", "Desayunar", "Come y bebe a tu ritmo.", "TAZA"),
    )

    val routines = listOf(
        Routine(
            id = "morning",
            title = "Prepararse para el día",
            category = RoutineCategory.MORNING,
            steps = morningSteps,
            scheduleLabel = "Hoy · 7:15",
        ),
        Routine(
            id = "backpack",
            title = "Preparar la mochila",
            category = RoutineCategory.AFTERNOON,
            steps = listOf(
                ActivityStep("notebooks", "Guardar cuadernos", "Coloca los cuadernos en la mochila.", "LIBROS"),
                ActivityStep("bottle", "Guardar agua", "Coloca tu botella en el bolsillo.", "AGUA"),
                ActivityStep("check", "Revisar la lista", "Mira si llevas todo lo necesario.", "LISTA"),
                ActivityStep("close", "Cerrar la mochila", "Cierra cada cremallera.", "MOCHILA"),
            ),
            scheduleLabel = "Tarde · 16:00",
        ),
        Routine(
            id = "quiet",
            title = "Ocio tranquilo",
            category = RoutineCategory.AS_NEEDED,
            steps = listOf(
                ActivityStep("choose", "Elegir una actividad", "Elige leer, dibujar o escuchar música.", "ELEGIR"),
                ActivityStep("enjoy", "Disfrutar la actividad", "Haz la actividad a tu ritmo.", "CALMA"),
                ActivityStep("finish", "Guardar materiales", "Deja cada objeto en su lugar.", "GUARDAR"),
            ),
            scheduleLabel = "Según necesidad",
        ),
    )

    val templates = listOf(
        routines.first().copy(id = "template-morning", isTemplate = true),
        Routine(
            id = "template-hands",
            title = "Lavado de manos",
            category = RoutineCategory.AS_NEEDED,
            steps = listOf(
                ActivityStep("wet", "Mojar las manos", "Abre el agua y moja tus manos.", "AGUA"),
                ActivityStep("soap", "Usar jabón", "Pon una pequeña cantidad de jabón.", "JABÓN"),
                ActivityStep("rub", "Frotar", "Frota palmas, dedos y uñas.", "MANOS"),
                ActivityStep("dry", "Enjuagar y secar", "Enjuaga y seca tus manos.", "TOALLA"),
            ),
            scheduleLabel = "Higiene · 4 pasos",
            isTemplate = true,
        ),
        Routine(
            id = "template-pause",
            title = "Pausa de regulación",
            category = RoutineCategory.AS_NEEDED,
            steps = listOf(
                ActivityStep("place", "Buscar un lugar tranquilo", "Ve a un espacio que te resulte cómodo.", "LUGAR"),
                ActivityStep("breathe", "Respirar", "Respira de la manera que te ayude.", "AIRE"),
                ActivityStep("choice", "Elegir qué sigue", "Puedes continuar o pedir apoyo.", "ELEGIR"),
            ),
            scheduleLabel = "Autorregulación · apoyo visual",
            isTemplate = true,
        ),
    )

    val history = listOf(
        ProgressRecord("Prepararse para el día", "Hoy · 7:22", RoutineOutcome.COMPLETED),
        ProgressRecord("Cierre del día", "Ayer · 20:31", RoutineOutcome.PARTIAL),
        ProgressRecord("Ocio tranquilo", "Ayer · 17:05", RoutineOutcome.CANCELLED),
    )
}
