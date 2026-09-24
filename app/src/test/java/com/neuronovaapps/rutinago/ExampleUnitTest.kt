package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.data.demo.DemoContent
import com.neuronovaapps.rutinago.data.institutional.InstitutionalLinks
import com.neuronovaapps.rutinago.domain.model.RoutineCategory
import com.neuronovaapps.rutinago.domain.model.filterFor
import com.neuronovaapps.rutinago.ui.navigation.AppDestination
import com.neuronovaapps.rutinago.ui.navigation.mainDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun routineSearch_matchesRoutineAndStepTitles() {
        val byRoutine = DemoContent.routines.filterFor("mochila", RoutineCategory.ALL)
        val byStep = DemoContent.routines.filterFor("lavarse", RoutineCategory.ALL)

        assertEquals(listOf("Preparar la mochila"), byRoutine.map { it.title })
        assertEquals(listOf("Prepararse para el día"), byStep.map { it.title })
    }

    @Test
    fun routineFilter_combinesCategoryAndQuery() {
        val results = DemoContent.routines.filterFor("preparar", RoutineCategory.AFTERNOON)

        assertEquals(1, results.size)
        assertTrue(results.single().category == RoutineCategory.AFTERNOON)
    }

    @Test
    fun mainNavigation_containsTheFiveRequiredSections() {
        assertEquals(
            listOf("Hoy", "Rutinas", "Crear", "Progreso", "Configuración"),
            mainDestinations.map { it.label },
        )
        assertTrue(mainDestinations.all(AppDestination::isMain))
        assertTrue(!AppDestination.EXECUTION.isMain)
        assertTrue(!AppDestination.ABOUT.isMain)
        assertTrue(AppDestination.ABOUT !in mainDestinations)
    }

    @Test
    fun institutionalLinks_useVerifiedSchemesAndContact() {
        assertTrue(InstitutionalLinks.NEURONOVA_APPS_WEBSITE.startsWith("https://"))
        assertEquals(
            "https://neuronova-apps.github.io/privacy/rutinago/",
            InstitutionalLinks.RUTINAGO_PRIVACY_POLICY,
        )
        assertEquals(
            "mailto:${InstitutionalLinks.CONTACT_EMAIL}",
            InstitutionalLinks.CONTACT_EMAIL_URI,
        )
    }
}
