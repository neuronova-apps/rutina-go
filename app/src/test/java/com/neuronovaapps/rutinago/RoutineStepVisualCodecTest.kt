package com.neuronovaapps.rutinago

import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageResolution
import com.neuronovaapps.rutinago.domain.arasaac.ArasaacImageUrls
import com.neuronovaapps.rutinago.domain.user.RoutineStepVisualCodec
import com.neuronovaapps.rutinago.domain.user.RoutineStepVisualReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineStepVisualCodecTest {
    @Test
    fun storesOnlyVersionedIdentityAndRebuildsImageUrl() {
        val encoded = RoutineStepVisualCodec.encode(
            RoutineStepVisualReference("ARASAAC", 10384, "lavarse las manos"),
        )
        assertFalse(encoded.contains("https:"))
        assertEquals(RoutineStepVisualReference("ARASAAC", 10384, "lavarse las manos"),
            RoutineStepVisualCodec.decode(encoded))
        assertEquals("https://static.arasaac.org/pictograms/10384/10384_300.png",
            ArasaacImageUrls.forPictogram(10384, ArasaacImageResolution.THUMBNAIL))
    }

    @Test
    fun rejectsInvalidIdsAndUnknownFormatsWithoutCrashingReader() {
        assertNull(RoutineStepVisualCodec.decode(null))
        assertNull(RoutineStepVisualCodec.decode("legacy"))
        assertNull(RoutineStepVisualCodec.decode("""{"schema":"rutinago.visual.v1","source":"ARASAAC","id":0,"label":"x"}"""))
        assertNull(RoutineStepVisualCodec.decode("""{"schema":"rutinago.visual.v1","source":"ARASAAC","id":-2,"label":"x"}"""))
        assertNull(RoutineStepVisualCodec.decode("""{"schema":"rutinago.visual.v1","source":"ARASAAC","id":1.5,"label":"x"}"""))
        assertNull(RoutineStepVisualCodec.decode("""{"schema":"rutinago.visual.v2","source":"ARASAAC","id":42,"label":"x"}"""))
    }
}
