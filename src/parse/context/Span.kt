package dev.kdl.parse.context

import kotlin.jvm.JvmRecord

/**
 * Represents consecutive characters in the input document.
 * 
 * @param start the position of the first character of the span
 * @param end   the position of the last character of the span (inclusive)
 */
@JvmRecord
data class Span(
    val start: Position,
    val end: Position,
) {
    companion object {
        /**
         * Creates a one character width span.
         *
         * @param position the start and end position
         * @return a new span
         */
        fun of(position: Position): Span = Span(position, position)

        /**
         * Creates a one character width span.
         *
         * @param line   the line of the span
         * @param column the column of the span
         * @return a new span
         */
        fun of(line: Int, column: Int): Span {
            val position = Position(line, column)
            return Span(position, position)
        }

        /**
         * Creates a multi-character span.
         *
         * @param startLine   the line of the first character of the span
         * @param startColumn the column of the first character of the span
         * @param endLine     the line of the last character of the span
         * @param endColumn   the column of the last character of the span
         * @return a new span
         */
        fun of(startLine: Int, startColumn: Int, endLine: Int, endColumn: Int): Span = Span(
            Position(startLine, startColumn),
            Position(endLine, endColumn)
        )
    }
}
