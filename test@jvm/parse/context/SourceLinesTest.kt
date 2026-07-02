package dev.kdl.parse.context

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SourceLinesTest {
    @Nested
    @DisplayName("getLines(int, int) should")
    internal inner class GetLines {
        @Test
        @DisplayName("return an empty list when lines are not present")
        fun missingLines() {
            val sourceLines = SourceLines()

            val lines = sourceLines.getLines(1, 3)

            assertTrue(lines.isEmpty(), "lines should be empty")
        }

        @Test
        @DisplayName("return a list with lines 1 to 2 when lines they are present")
        fun lines1to2() {
            val sourceLines = SourceLines()
            addLine(sourceLines, "line 1")
            addLine(sourceLines, "line 2")

            val lines = sourceLines.getLines(1, 2)

            assertEquals(
                listOf(
                    SourceLine(1, "line 1"),
                    SourceLine(2, "line 2"),
                ),
                lines,
            )
        }

        @Test
        @DisplayName("return a list with lines 1 to 2 when lines 1 to 3 are requested but only two lines are present")
        fun lines1to3WithOnly2() {
            val sourceLines = SourceLines()
            addLine(sourceLines, "line 1")
            addLine(sourceLines, "line 2")

            val lines = sourceLines.getLines(1, 3)

            assertEquals(
                listOf(
                    SourceLine(1, "line 1"),
                    SourceLine(2, "line 2"),
                ),
                lines,
            )
        }

        @Test
        @DisplayName("return lines from 1 when startLine is 0")
        fun invalidStartLine() {
            val sourceLines = SourceLines()
            addLine(sourceLines, "line 1")
            addLine(sourceLines, "line 2")

            val lines = sourceLines.getLines(0, 2)

            assertEquals(
                listOf(
                    SourceLine(1, "line 1"),
                    SourceLine(2, "line 2"),
                ),
                lines,
            )
        }

        @Test
        @DisplayName("return an empty list when startLine is greater than endLine")
        fun startLineGreaterThanEndLine() {
            val sourceLines = SourceLines()
            val lines = sourceLines.getLines(2, 1)

            assertTrue(lines.isEmpty(), "lines should be empty")
        }
    }

    private fun addLine(sourceLines: SourceLines, line: String) {
        for (c in line.toByteArray(java.nio.charset.StandardCharsets.UTF_8)) {
            sourceLines.append(c.toInt())
        }
        sourceLines.newline()
    }
}
