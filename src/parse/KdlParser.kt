package dev.kdl.parse

import dev.kdl.KdlDocument
import dev.kdl.KdlVersion
import kotlinx.io.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * A parser for KDL documents.
 */
interface KdlParser {
	/**
	 * Parses a source as a [KdlDocument].
	 *
	 * @param filename    the name of the parsed file
	 * @param source      an input stream returning a KDL document
	 * @return a [KdlDocument] corresponding to `document`
	 * @throws IOException       if an error occurs while reading the input
	 * @throws KdlParseException if the document is invalid
	 */
    @Throws(IOException::class, KdlParseException::class)
    fun parse(source: Source, filename: String? = null): KdlDocument

    /**
     * Parses a file as a [KdlDocument].
     *
     * @param path path to a file containing a KDL document
     * @return a [KdlDocument] corresponding to `document`
     * @throws IOException       if an error occurs while reading the input
     * @throws KdlParseException if the document is invalid
     */
    @Throws(IOException::class, KdlParseException::class)
    fun parse(path: Path): KdlDocument {
        SystemFileSystem.source(path).buffered().use {
            return parse(it, path.toString())
        }
    }

    /**
     * Parses a string as a [KdlDocument].
     *
     * @param filename the name of the parsed file
     * @param document a string representation of a KDL document
     * @return a [KdlDocument] corresponding to `document`
     * @throws IOException       if an error occurs while reading the input
     * @throws KdlParseException if the document is invalid
     */
    @Throws(IOException::class, KdlParseException::class)
    fun parse(document: String, filename: String? = null): KdlDocument {
        Buffer().use { buffer ->
            buffer.writeString(document)
            return parse(buffer, filename)
        }
    }

    companion object {
        private val KDL1_PARSER: Kdl1Parser by lazy { Kdl1Parser() }
        private val KDL2_PARSER: Kdl2Parser by lazy { Kdl2Parser() }
        private val KDL_HYBRID_PARSER: KdlHybridParser by lazy { KdlHybridParser() }

		/**
		 * Creates a new KDL 1 parser.
		 *
		 * @return a KDL 1 parser
		 */
		val v1: KdlParser = KDL1_PARSER

		/**
		 * Creates a new KDL 2 parser.
		 *
		 * @return a KDL 2 parser
		 */
		val v2: KdlParser = KDL2_PARSER

		/**
		 * Creates a new parser in hybrid mode: it first tries to parse the document as a KDL 2 document, then tries to
		 * parse it as a KDL 1 document if it fails.
		 *
		 * @return a hybrid KDL parser
		 */
		val hybrid: KdlParser = KDL_HYBRID_PARSER

		/**
		 * Creates a new parser depending on the specified version. If no version is specified, creates a hybrid parser.
		 *
		 * @param version the version of the created parser
		 * @return a new parser
		 */
		fun fromVersion(version: KdlVersion?): KdlParser = when (version) {
			null -> hybrid
			KdlVersion.V1 -> v1
			KdlVersion.V2 -> v2
		}
    }
}
