package com.neuronovaapps.rutinago.data.arasaac.offline

import java.io.File
import java.io.FileInputStream
import java.io.DataInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

data class PictogramStorageStats(val fileCount: Int, val totalBytes: Long)

class InvalidPictogramException(message: String) : IOException(message)
class IncompletePictogramException : IOException("La descarga del pictograma quedó incompleta.")
class PictogramStorageException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Owns only reconstructible PNGs in the app's private files directory. */
class PictogramFileStore(private val directory: File) {
    fun fileFor(id: Long): File {
        require(id > 0) { "El ID de ARASAAC debe ser positivo." }
        return File(directory, "$id.png")
    }

    fun getLocalFile(id: Long): File? = fileFor(id).takeIf(::isValidPng)

    fun exists(id: Long): Boolean = getLocalFile(id) != null

    fun save(id: Long, input: InputStream, expectedBytes: Long = -1): File {
        require(id > 0) { "El ID de ARASAAC debe ser positivo." }
        getLocalFile(id)?.let { input.close(); return it }
        if (expectedBytes == 0L || expectedBytes > MAX_BYTES) {
            input.close()
            throw InvalidPictogramException("Tamaño de pictograma no válido.")
        }
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            input.close()
            throw PictogramStorageException("No se pudo crear el directorio privado de pictogramas.")
        }
        val temporary = try {
            File.createTempFile("$id-p", ".part", directory)
        } catch (error: IOException) {
            input.close()
            throw PictogramStorageException("No se pudo crear el archivo temporal.", error)
        }
        try {
            var copied = 0L
            input.use { source ->
                val output = try {
                    FileOutputStream(temporary)
                } catch (error: IOException) {
                    throw PictogramStorageException("No se pudo abrir el archivo temporal.", error)
                }
                output.use {
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        copied += read
                        if (copied > MAX_BYTES) {
                            throw InvalidPictogramException("El pictograma supera el límite de 5 MB.")
                        }
                        try {
                            output.write(buffer, 0, read)
                        } catch (error: IOException) {
                            throw PictogramStorageException("No se pudo escribir el pictograma.", error)
                        }
                    }
                    try {
                        output.fd.sync()
                    } catch (error: IOException) {
                        throw PictogramStorageException("No se pudo confirmar el archivo.", error)
                    }
                }
            }
            if (expectedBytes >= 0 && copied != expectedBytes) throw IncompletePictogramException()
            if (!isValidPng(temporary)) throw InvalidPictogramException("El archivo no es un PNG completo.")
            val destination = fileFor(id)
            synchronized(commitLock) {
                getLocalFile(id)?.let { return it }
                if (destination.exists() && !destination.delete()) {
                    throw PictogramStorageException("No se pudo reemplazar un pictograma inválido.")
                }
                if (!temporary.renameTo(destination)) {
                    throw PictogramStorageException("No se pudo publicar el pictograma descargado.")
                }
            }
            return destination
        } finally {
            temporary.delete()
        }
    }

    fun stats(): PictogramStorageStats {
        val files = directory.listFiles().orEmpty().filter { file ->
            file.name.matches(Regex("[1-9][0-9]*\\.png")) && isValidPng(file)
        }
        return PictogramStorageStats(files.size, files.sumOf(File::length))
    }

    private fun isValidPng(file: File): Boolean {
        if (!file.isFile || file.length() !in MIN_PNG_BYTES..MAX_BYTES) return false
        return runCatching {
            DataInputStream(FileInputStream(file)).use { stream ->
                val header = ByteArray(24)
                stream.readFully(header)
                if (!header.copyOfRange(0, 8).contentEquals(PNG_SIGNATURE)) return false
                if (header.copyOfRange(8, 16).contentEquals(IHDR_HEADER).not()) return false
                val width = intAt(header, 16)
                val height = intAt(header, 20)
                if (width !in 1..MAX_DIMENSION || height !in 1..MAX_DIMENSION) return false
            }
            java.io.RandomAccessFile(file, "r").use { random ->
                random.seek(file.length() - IEND_SUFFIX.size)
                val footer = ByteArray(IEND_SUFFIX.size)
                random.readFully(footer)
                footer.contentEquals(IEND_SUFFIX)
            }
        }.getOrDefault(false)
    }

    private fun intAt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xff) shl 24) or
            ((bytes[offset + 1].toInt() and 0xff) shl 16) or
            ((bytes[offset + 2].toInt() and 0xff) shl 8) or
            (bytes[offset + 3].toInt() and 0xff)

    companion object {
        const val MAX_BYTES = 5L * 1024 * 1024
        private const val MIN_PNG_BYTES = 45L
        private const val MAX_DIMENSION = 2500
        private val commitLock = Any()
        private val PNG_SIGNATURE = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
        private val IHDR_HEADER = byteArrayOf(0, 0, 0, 13, 73, 72, 68, 82)
        private val IEND_SUFFIX = byteArrayOf(0, 0, 0, 0, 73, 69, 78, 68, -82, 66, 96, -126)

        fun fromFilesDir(filesDir: File): PictogramFileStore =
            PictogramFileStore(File(filesDir, "pictograms/arasaac"))
    }
}
