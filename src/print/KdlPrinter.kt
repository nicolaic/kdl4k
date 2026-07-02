package dev.kdl.print

import dev.kdl.KdlDocument
import dev.kdl.KdlVersion
import kotlinx.io.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.jvm.JvmStatic

/**
 * Entry point for printing a KDL document.
 */
class KdlPrinter(
    private val configuration: KdlPrinterConfiguration = KdlPrinterConfiguration(),
) {
    /**
     * Prints a document into a string.
     *
     * @param document the document to print
     * @return the printed document, in a [String]
     * @throws IOException never
     * @throws IOException when an error occurs while writing
     */
    @Throws(IOException::class)
    fun printToString(document: KdlDocument): String = Buffer().use { buffer ->
        getContext(buffer, configuration).printDocument(document)
        return buffer.readString()
    }

    /**
     * Prints a document to a [Sink].
     *
     * @param document the document to print
     * @param sink     the sink to write to
     * @throws IOException when an error occurs while writing
     */
    @Throws(IOException::class)
    fun print(document: KdlDocument, sink: Sink) {
        getContext(sink, configuration).printDocument(document)
    }

    /**
     * Prints a document to a file.
     *
     * @param document the document to print
     * @param path     the path of the file to write to
     * @throws IOException when an error occurs while writing
     */
    @Throws(IOException::class)
    fun print(document: KdlDocument, path: Path) {
        return SystemFileSystem.sink(path).buffered().use { sink ->
            getContext(sink, configuration).printDocument(document)
        }
    }

    private fun getContext(sink: Sink, configuration: KdlPrinterConfiguration): KdlPrinterContext =
        when (configuration.version) {
            KdlVersion.V1 -> Kdl1PrinterContext(sink, configuration)
            KdlVersion.V2 -> Kdl2PrinterContext(sink, configuration)
        }

    companion object {
		@JvmStatic val v1: KdlPrinter = KdlPrinter(KdlPrinterConfiguration(version = KdlVersion.V1))
		@JvmStatic val v2: KdlPrinter = KdlPrinter()
	}
}
