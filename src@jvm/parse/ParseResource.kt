package dev.kdl.parse

import dev.kdl.KdlDocument
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.IOException

/**
 * Reads a resource using [ClassLoader.getSystemResourceAsStream] and parses it as a
 * [KdlDocument].
 *
 * @param resource the name of the resource to parse
 * @return a [KdlDocument]
 * @throws IOException       if an error occurs while reading the input or the resource is not found
 * @throws KdlParseException if the document is invalid
 */
@Throws(IOException::class, KdlParseException::class)
fun KdlParser.parseResource(resource: String): KdlDocument {
    ClassLoader.getSystemResourceAsStream(resource).use { inputStream ->
        if (inputStream == null) throw IOException("Resource $resource was not found")
        return parse(inputStream.asSource().buffered(), resource)
    }
}
