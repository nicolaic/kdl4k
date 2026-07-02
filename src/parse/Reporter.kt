package dev.kdl.parse

import dev.kdl.parse.context.ParseContext
import dev.kdl.parse.context.SourceLine
import dev.kdl.parse.context.Span
import kotlin.jvm.JvmStatic

/**
 * A reporter for [KdlParseException]. The goal is to display a user-friendly message, with the context of the
 * error and as much information or help as possible.
 *
 *
 * The reporter can optionally return a string including ANSI escape codes to display colors.
 */
class Reporter private constructor(
    parseException: KdlParseException,
    private val ansiColors: Boolean,
) {
    private val report: String
        get() {
            if (builder.isNotEmpty()) return builder.toString()

            printErrorSign()
            print(message)

            if (context != null) printContext(context)
            if (help != null) printHelpMessage()

            return builder.toString()
        }

    private fun printContext(context: ParseContext) {
        print(":\n")

        printMargin("╭─[")
        printFilename(context)
        print(context.span.start.line)
        print(":")
        print(context.span.start.column)
        print("]\n")

        for (sourceLine in context.sourceLines) {
            printMargin(sourceLine.lineNumber)
            print(sourceLine.line)
            if (label != null) {
                printUnderline(sourceLine, context.span)
            }
            println()
        }

        printMargin("╰─")
    }

    private fun printFilename(context: ParseContext) {
        val filename = context.filename

        if (filename != null) {
            printWithColor(filename, ANSI_CYAN_UNDERLINE)
            print(":")
        }
    }

    private fun printUnderline(line: SourceLine, span: Span) {
        val isSpanStartLine = span.start.line == line.lineNumber
        val start = if (isSpanStartLine) span.start.column else 1
        if (span.start.line <= line.lineNumber && span.end.line > line.lineNumber) {
            val end = line.line.length
            println()
            printMargin("· ")
            print(" ".repeat(start - 1))
            printArrow("─".repeat(end - start + 1))
        } else if (span.end.line == line.lineNumber) {
            val end = span.end.column
            val verticalBarColumn: Int = start + (end - start) / 2
            println()
            printMargin("· ")
            print(" ".repeat(start - 1))
            printArrow("─".repeat(verticalBarColumn - start) + "┬" + "─".repeat(end - verticalBarColumn))

            if (label!!.length < verticalBarColumn - 2) {
                println()
                printMargin("· ")
                print(" ".repeat(verticalBarColumn - 2 - label.length))
                printArrow("$label ╯")
            } else {
                println()
                printMargin("· ")
                print(" ".repeat(verticalBarColumn - 1))
                printArrow("╰ $label")
            }
        }
    }

    private fun printHelpMessage() {
        println()
        printHelp()
        print(help)
    }

    private fun printMargin(line: Int) {
        print(line.toString().padStart(lineNumberSize))
        print(" │ ")
    }

    private fun printMargin(separator: String?) {
        print(" ".repeat(lineNumberSize + 1))
        print(separator)
    }

    private fun printErrorSign() {
        printWithColor("× ", ANSI_ERROR)
    }

    private fun printArrow(content: String?) {
        printWithColor(content, ANSI_ARROW)
    }

    private fun printHelp() {
        if (ansiColors) {
            print(ANSI_BLUE_UNDERLINE)
        }
        print("help")
        if (ansiColors) {
            print(ANSI_REMOVE_UNDERLINE)
        }
        print(": ")
        if (ansiColors) {
            print(ANSI_RESET)
        }
    }

    private fun printWithColor(content: String?, color: String?) {
        if (ansiColors) {
            print(color)
        }
        print(content)
        if (ansiColors) {
            print(ANSI_RESET)
        }
    }

    private fun print(content: Any?) {
        builder.append(content)
    }

    private fun println() {
        builder.append('\n')
    }

    private val builder = StringBuilder()
    private val message: String = parseException.errorMessage
	private val context = parseException.context
	private val label: String? = parseException.label
	private val help: String? = parseException.help
	private val lineNumberSize: Int

    init {
		if (context != null) {
            val sourceLines = context.sourceLines
            val lastLine = sourceLines[sourceLines.size - 1]
            lineNumberSize = lastLine.lineNumber.toString().length
        } else {
            lineNumberSize = 0
        }
    }

    companion object {
        /**
         * Creates a report for an exception, without ANSI escape codes.
         *
         * @param parseException the exception to create a report of
         * @return a user-friendly error report
         */
        @JvmStatic
        fun getReport(parseException: KdlParseException): String {
            return getReport(parseException, false)
        }

        /**
         * Creates a report for an exception, with ANSI escape codes.
         *
         * @param parseException the exception to create a report of
         * @return a user-friendly error report with ANSI colors
         */
        fun getReportWithAnsiCodes(parseException: KdlParseException): String {
            return getReport(parseException, true)
        }

        /**
         * Creates a report for an exception.
         *
         * @param parseException the exception to create a report of
         * @param ansiColors     whether to include ANSI escape codes for color
         * @return a user-friendly error report
         */
        fun getReport(parseException: KdlParseException, ansiColors: Boolean): String {
            if (parseException is KdlHybridParseException) {
                return "Failed to parse the document using both the KDL v2 and the KDL v1 parser.\n" +
                        "KDL v2 error:\n" + Reporter(parseException.v1Exception, ansiColors).report + "\n" +
                        "KDL v1 error:\n" + Reporter(parseException.v2Exception, ansiColors).report
            }
            return Reporter(parseException, ansiColors).report
        }

        private const val ANSI_RESET = "\u001B[0m"
        private const val ANSI_ERROR = "\u001B[1;91m"
        private const val ANSI_ARROW = "\u001B[35m"
        private const val ANSI_BLUE_UNDERLINE = "\u001B[1;4;94m"
        private const val ANSI_CYAN_UNDERLINE = "\u001B[1;4;96m"
        private const val ANSI_REMOVE_UNDERLINE = "\u001B[24m"
    }
}
