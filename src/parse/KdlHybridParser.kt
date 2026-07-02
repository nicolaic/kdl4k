package dev.kdl.parse

import dev.kdl.KdlDocument
import kotlinx.io.IOException
import kotlinx.io.Source

/**
 * A hybrid parser that first tries to parse a document using KDL 2.0 syntax, but switches to the KDL 1.0 syntax if it
 * fails. If both parsers fail, a [KdlHybridParseException] is thrown, containing both errors.
 */
class KdlHybridParser : KdlParser {

    @Throws(IOException::class, KdlParseException::class)
    override fun parse(source: Source, filename: String?): KdlDocument = try {
        KdlParser.v2.parse(source, filename)
    } catch (v2Exception: KdlParseException) {
        try {
            KdlParser.v1.parse(source, filename)
        } catch (v1Exception: KdlParseException) {
            throw KdlHybridParseException(v1Exception, v2Exception)
        }
    }
}
