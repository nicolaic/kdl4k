package dev.kdl.parse.lexer.reader

import kotlinx.io.IOException

/**
 * This exception is thrown when the reader fails to read a valid Unicode codepoint.
 */
class KdlReadException(message: String) : IOException(message)
