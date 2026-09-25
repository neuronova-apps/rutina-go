package com.neuronovaapps.rutinago.data.catalog.local

import androidx.room.TypeConverter
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogImportStatus
import com.neuronovaapps.rutinago.data.catalog.local.entity.CatalogReferenceType
import com.neuronovaapps.rutinago.data.catalog.local.entity.GamificationOwnerType
import com.neuronovaapps.rutinago.data.catalog.local.entity.RoutineVariant

class CatalogDatabaseConverters {
    @TypeConverter
    fun routineVariantToString(value: RoutineVariant): String = value.name

    @TypeConverter
    fun stringToRoutineVariant(value: String): RoutineVariant = RoutineVariant.valueOf(value)

    @TypeConverter
    fun ownerTypeToString(value: GamificationOwnerType): String = value.name

    @TypeConverter
    fun stringToOwnerType(value: String): GamificationOwnerType =
        GamificationOwnerType.valueOf(value)

    @TypeConverter
    fun referenceTypeToString(value: CatalogReferenceType): String = value.name

    @TypeConverter
    fun stringToReferenceType(value: String): CatalogReferenceType =
        CatalogReferenceType.valueOf(value)

    @TypeConverter
    fun importStatusToString(value: CatalogImportStatus): String = value.name

    @TypeConverter
    fun stringToImportStatus(value: String): CatalogImportStatus =
        CatalogImportStatus.valueOf(value)
}
