package dev.kdl.parse.context

import dev.kdl.parse.KdlInternalParseException
import kotlin.math.max
import kotlin.math.min

/**
 * A class for storing the lines of the input. Mainly used for error reporting.
 */
class SourceLines {
    private val lines: MutableList<String> = mutableListOf()
    private val line = StringBuilder()
    private var currentLine = 0
    private var currentColumn = 0
    private var nextLine = 1
    private var nextColumn = 1

    /**
     * Appends a codepoint to the current line and computes its position.
     *
     * @param c the codepoint to append
     */
    fun append(c: Int) {
        line.append(c.toChar())
        currentLine = nextLine
        currentColumn = nextColumn
        nextColumn += 1
    }

    /**
     * Indicates a newline character has been reached. It does not append any character to the lines.
     */
    fun newline() {
        lines.add(line.toString().trimEnd())
        line.setLength(0)
        nextLine += 1
        nextColumn = 1
    }

    fun getCurrentPosition(): Position {
        if (currentLine == 0 || currentColumn == 0) {
            throw KdlInternalParseException("current position cannot be returned yet")
        }

        return Position(currentLine, currentColumn)
    }

    fun getNextPosition(): Position = Position(nextLine, nextColumn)

    /**
     * Get a list of [SourceLine]. If some lines are missing, only lines that are present and requested are
     * returned.
     *
     * @param startLine the first line to retrieve
     * @param endLine   the last line to retrieve (included)
     * @return a list of lines according to the requested lines
     */
    fun getLines(startLine: Int, endLine: Int): List<SourceLine> =
        (max(1, startLine)..<min(lines.size, endLine) + 1).map { lineNumber ->
            SourceLine(lineNumber, lines[lineNumber - 1])
        }
}
