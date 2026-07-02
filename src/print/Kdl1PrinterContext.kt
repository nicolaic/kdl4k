package dev.kdl.print

import dev.kdl.KdlBoolean
import dev.kdl.KdlNull
import dev.kdl.KdlNumber
import dev.kdl.KdlString
import dev.kdl.parse.lexer.helper.Kdl1CharHelper
import dev.kdl.parse.lexer.helper.KdlCharHelper.isDecimalDigit
import kotlinx.io.Sink

/**
 * Printer context for KDL 1.0 syntax.
 */
class Kdl1PrinterContext(
    sink: Sink,
    configuration: KdlPrinterConfiguration,
) : KdlPrinterContext(sink, configuration) {
    override fun isIdentifierChar(c: Int): Boolean {
        return Kdl1CharHelper.isIdentifierChar(c)
    }

    override fun printString(string: KdlString) {
        printType(string.type)
        writeString(string.value)
    }

    private fun writeString(string: String) {
        if (string.isEmpty()) {
            write("\"\"")
        } else {
            writeEscapedString(string, printQuotes = true)
        }
    }

    override fun printNull(kdlNull: KdlNull) {
        printType(kdlNull.type)
        write("null")
    }

    override fun printBoolean(kdlBoolean: KdlBoolean) {
        printType(kdlBoolean.type)
        write(if (kdlBoolean.value) "true" else "false")
    }

    override fun printNotANumber(notANumber: KdlNumber.NotANumber) {
        throw UnsupportedOperationException("KDL v1 does not support Not A Number")
    }

    override fun printPositiveInfinity(positiveInfinity: KdlNumber.PositiveInfinity) {
        throw UnsupportedOperationException("KDL v1 does not support Positive Infinity")
    }

    override fun printNegativeInfinity(negativeInfinity: KdlNumber.NegativeInfinity) {
        throw UnsupportedOperationException("KDL v1 does not support Negative Infinity")
    }

    override fun isValidStartOfIdentifier(c: Int): Boolean {
        return isIdentifierChar(c) && !isDecimalDigit(c)
    }
}
