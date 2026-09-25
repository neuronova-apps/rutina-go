package com.neuronovaapps.rutinago

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class MasterBankArtifactsTest {
    private val generatedDir: File by lazy {
        val workingDirectory = requireNotNull(System.getProperty("user.dir"))
        var current: File? = File(workingDirectory).absoluteFile
        while (current != null) {
            val candidate = File(current, "data/master_bank/generated/v1")
            if (File(candidate, "manifest.json").isFile) return@lazy candidate
            current = current.parentFile
        }
        error("Unable to locate data/master_bank/generated/v1")
    }

    private fun read(name: String): String =
        File(generatedDir, name).readText(Charsets.UTF_8)

    private val packagedAssetsDir: File
        get() {
            val repositoryRoot = requireNotNull(
                generatedDir.parentFile?.parentFile?.parentFile?.parentFile,
            )
            return File(repositoryRoot, "app/src/main/assets/master_catalog/v1")
        }

    @Test
    fun generatedArtifactsKeepAuthorizedCountsAndIds() {
        val manifest = read("manifest.json")
        val expectedCounts = mapOf(
            "activities" to 203,
            "routines" to 52,
            "achievements" to 40,
            "challenges" to 20,
            "reinforcementMessages" to 54,
            "masterRecords" to 369,
        )
        expectedCounts.forEach { (key, value) ->
            assertTrue("$key must equal $value", manifest.contains("\"$key\": $value"))
        }

        val idExpectations = mapOf(
            "activities.json" to Pair(Regex("\"id\": \"(ACT-\\d{3})\""), 203),
            "routines.json" to Pair(Regex("\"id\": \"(RUT-\\d{3})\""), 52),
            "achievements.json" to Pair(Regex("\"id\": \"(LOG-\\d{3})\""), 40),
            "challenges.json" to Pair(Regex("\"id\": \"(RET-\\d{3})\""), 20),
            "reinforcement_messages.json" to
                Pair(Regex("\"id\": \"(MSG-\\d{3})\""), 54),
        )
        idExpectations.forEach { (filename, expectation) ->
            val ids = expectation.first.findAll(read(filename)).map { it.groupValues[1] }.toList()
            assertEquals(filename, expectation.second, ids.size)
            assertEquals("$filename IDs must be unique", ids.size, ids.toSet().size)
        }
    }

    @Test
    fun routineStepsReferenceRealIdsAndKeepContiguousPositions() {
        val activityIds = Regex("\"id\": \"(ACT-\\d{3})\"")
            .findAll(read("activities.json"))
            .map { it.groupValues[1] }
            .toSet()
        val routineIds = Regex("\"id\": \"(RUT-\\d{3})\"")
            .findAll(read("routines.json"))
            .map { it.groupValues[1] }
            .toSet()
        val stepPattern = Regex(
            """\{\s*"activityId": "(ACT-\d{3})",\s*"position": (\d+),\s*""" +
                """"routineId": "(RUT-\d{3})",\s*"sequenceType": """ +
                """"(SIMPLE|COMPLETE|OPTIONAL)"\s*\}""",
        )
        val groupedPositions = linkedMapOf<Pair<String, String>, MutableList<Int>>()
        val matches = stepPattern.findAll(read("routine_steps.json")).toList()
        assertEquals(490, matches.size)
        matches.forEach { match ->
            val activityId = match.groupValues[1]
            val position = match.groupValues[2].toInt()
            val routineId = match.groupValues[3]
            val sequenceType = match.groupValues[4]
            assertTrue(activityId, activityId in activityIds)
            assertTrue(routineId, routineId in routineIds)
            groupedPositions.getOrPut(routineId to sequenceType) { mutableListOf() }
                .add(position)
        }
        groupedPositions.forEach { (key, positions) ->
            assertEquals(key.toString(), (1..positions.size).toList(), positions)
        }
    }

    @Test
    fun pendingVisualsContainNoInventedArasaacIds() {
        val visuals = read("visual_references.json")
        assertEquals(203, Regex("\"visualSource\": \"PENDING\"").findAll(visuals).count())
        assertEquals(203, Regex("\"arasaacId\": null").findAll(visuals).count())
        assertFalse(Regex("\"arasaacId\": \\d+").containsMatchIn(visuals))
    }

    @Test
    fun manifestChecksumsMatchGeneratedArtifacts() {
        val manifest = read("manifest.json")
        val artifactPattern = Regex(
            """\"([a-z_]+\.json)\": \{\s*\"itemCount\": \d+,\s*""" +
                """\"sha256\": \"([0-9a-f]{64})\"""",
        )
        val entries = artifactPattern.findAll(manifest)
            .associate { it.groupValues[1] to it.groupValues[2] }
        assertEquals(8, entries.size)
        entries.forEach { (filename, expectedHash) ->
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(File(generatedDir, filename).readBytes())
                .joinToString("") { byte -> "%02x".format(byte) }
            assertEquals(filename, expectedHash, digest)
        }
    }

    @Test
    fun androidAssetsAreByteIdenticalToValidatedGeneratedArtifacts() {
        val expectedFiles = generatedDir.listFiles().orEmpty().map { it.name }.sorted()
        val packagedFiles = packagedAssetsDir.listFiles().orEmpty().map { it.name }.sorted()
        assertEquals(expectedFiles, packagedFiles)
        expectedFiles.forEach { filename ->
            assertTrue(
                filename,
                File(generatedDir, filename).readBytes().contentEquals(
                    File(packagedAssetsDir, filename).readBytes(),
                ),
            )
        }
    }
}
