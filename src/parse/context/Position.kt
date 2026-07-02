package dev.kdl.parse.context

import kotlin.jvm.JvmRecord

/**
 * Represents a position in a file or stream.
 * 
 * @param line   the line for this position, 1-based
 * @param column the column for this position, 1-based
 */
@JvmRecord
data class Position(
    val line: Int,
    val column: Int,
) {
    /**
     * Creates a new position by adding an offset to the column. The line is unchanged.
     * 
     * @param columnOffset the offset to add to the column
     * @return a new position with the new column
     */
    fun withColumnOffset(columnOffset: Int): Position {
        return Position(line, column + columnOffset)
    }
}
