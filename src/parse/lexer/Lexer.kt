package dev.kdl.parse.lexer

import dev.kdl.parse.KdlParseException
import dev.kdl.parse.context.ParseContext
import dev.kdl.parse.context.Position
import dev.kdl.parse.context.Span
import dev.kdl.parse.lexer.token.Token
import kotlinx.io.IOException

/**
 * A KDL lexer.
 */
interface Lexer {
    /**
     * Reads the next token and advances the lexer.
     *
     * @return the next token, or `null` if the end of the file has been reached
     * @throws IOException       when an error occurs reading the input
     * @throws KdlParseException when the parsed document is not valid
     */
    @Throws(IOException::class, KdlParseException::class)
    fun read(): Token?

    /**
     * Peeks a token, without advancing the lexer. It is 0-based, therefore `peek(0)` is the same as `peek()`.
     *
     * @param n the index of the token to peek
     * @return the nth token, or `null` if the end of the file has been reached
     * @throws IOException       when an error occurs reading the input
     * @throws KdlParseException when the parsed document is not valid
     */
    @Throws(IOException::class, KdlParseException::class)
    fun peek(n: Int = 0): Token?

    /**
     * Creates a parse context to attach to an error. The current line is entirely read, to correctly display it.
     *
     * @param startLine the first line to include in the context
     * @param endLine   the last line to include in the context
     * @param span      the span to highlight
     * @return a parse context
     * @throws IOException when an error occurs reading the input
     */
    @Throws(IOException::class)
    fun getErrorParseContext(startLine: Int, endLine: Int, span: Span): ParseContext

    /**
     * Creates a parse context to attach to an error. The current line is entirely read, to correctly display it.
     *
     * @param span the span to highlight
     * @return a parse context
     * @throws IOException when an error occurs reading the input
     */
    @Throws(IOException::class)
    fun getErrorParseContext(span: Span): ParseContext {
        return getErrorParseContext(span.start.line, span.end.line, span)
    }

    @Throws(IOException::class)
    fun getErrorParseContextForNextPosition(): ParseContext

    /**
     * @return the starting position of the next token
     */
    fun getNextPosition(): Position
}
