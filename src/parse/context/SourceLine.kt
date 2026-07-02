package dev.kdl.parse.context

import kotlin.jvm.JvmRecord

/**
 * Represents a line in the input.
 * 
 * @param lineNumber the number of the line
 * @param line       the content of the line
 */
@JvmRecord
data class SourceLine(
    val lineNumber: Int,
    val line: String,
)
