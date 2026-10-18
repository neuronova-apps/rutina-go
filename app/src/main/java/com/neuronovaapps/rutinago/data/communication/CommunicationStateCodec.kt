package com.neuronovaapps.rutinago.data.communication

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.neuronovaapps.rutinago.domain.communication.CommunicationMessageBuilder
import com.neuronovaapps.rutinago.domain.communication.CommunicationMessageItem
import com.neuronovaapps.rutinago.domain.communication.CommunicationUiState

/** Bundle-compatible snapshot, never written to a file, preferences, or database. */
object CommunicationStateCodec {
    fun encode(state: CommunicationUiState): String = JsonObject().apply {
        addProperty("version", 1)
        add("categoryId", state.selectedCategoryId?.let { com.google.gson.JsonPrimitive(it) } ?: JsonNull.INSTANCE)
        addProperty("nextSelectionId", state.nextSelectionId)
        add("message", encodeItems(state.messageItems))
        add("clearedMessage", encodeItems(state.clearedItems))
    }.toString()

    fun decode(value: String, builder: CommunicationMessageBuilder): CommunicationUiState {
        val root = JsonParser.parseString(value).asJsonObject
        require(root["version"].asInt == 1) { "Unsupported communication session version" }
        return builder.restore(
            categoryId = root["categoryId"].takeUnless { it.isJsonNull }?.asString,
            messageItems = decodeItems(root.getAsJsonArray("message")),
            clearedItems = decodeItems(root.getAsJsonArray("clearedMessage")),
            nextSelectionId = root["nextSelectionId"].asLong,
        )
    }

    private fun encodeItems(items: List<CommunicationMessageItem>) = JsonArray().apply {
        items.forEach { item ->
            add(JsonObject().apply {
                addProperty("selectionId", item.selectionId)
                addProperty("vocabularyId", item.vocabularyId)
                addProperty("text", item.text)
            })
        }
    }

    private fun decodeItems(items: JsonArray): List<CommunicationMessageItem> = items.map {
        val row = it.asJsonObject
        CommunicationMessageItem(row["selectionId"].asLong, row["vocabularyId"].asString, row["text"].asString)
    }
}
