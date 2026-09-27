package com.neuronovaapps.rutinago.data.arasaac.offline

import com.neuronovaapps.rutinago.data.catalog.local.RutinaGoDatabase
import com.neuronovaapps.rutinago.domain.user.RoutineStepVisualCodec
import com.neuronovaapps.rutinago.domain.user.RoutineStepVisualReference

/** Includes archived personal steps and execution snapshots across every profile. */
class PictogramReferenceRepository(private val database: RutinaGoDatabase) {
    suspend fun allReferencedIds(): Set<Long> = sequenceOf(
        database.userRoutineDao().allVisualReferences(),
        database.routineExecutionDao().allVisualReferences(),
    ).flatten().mapNotNull(RoutineStepVisualCodec::decode)
        .filter { it.source == RoutineStepVisualReference.SOURCE_ARASAAC }
        .map { it.pictogramId }
        .toSet()

    suspend fun isPictogramReferenced(id: Long): Boolean {
        require(id > 0) { "El ID de ARASAAC debe ser positivo." }
        return id in allReferencedIds()
    }
}
