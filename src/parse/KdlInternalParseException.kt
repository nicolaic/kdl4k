package dev.kdl.parse

/**
 * Thrown if an unexpected state is encountered while parsing a document. If you encounter this
 * please report an issue with the offending document.
 */
class KdlInternalParseException(message: String?) : RuntimeException(message)
