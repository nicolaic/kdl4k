package dev.kdl.print

import dev.kdl.KdlBoolean
import dev.kdl.KdlNull
import dev.kdl.KdlNumber
import dev.kdl.parse.lexer.helper.Kdl2CharHelper
import dev.kdl.parse.lexer.helper.Kdl2CharHelper.isUnambiguousIdentifierChar
import dev.kdl.parse.lexer.helper.KdlCharHelper.isSign
import kotlinx.io.Sink

/**
 * Printer context for KDL 2.0 syntax.
 */
class Kdl2PrinterContext(
    sink: Sink,
    configuration: KdlPrinterConfiguration,
) : KdlPrinterContext(sink, configuration) {
    override fun isIdentifierChar(c: Int): Boolean {
        return Kdl2CharHelper.isIdentifierChar(c)
    }

    override fun printNull(kdlNull: KdlNull) {
        printType(kdlNull.type)
        write("#null")
    }

    override fun printBoolean(kdlBoolean: KdlBoolean) {
        printType(kdlBoolean.type)
        write(if (kdlBoolean.value) "#true" else "#false")
    }

    override fun printNotANumber(notANumber: KdlNumber.NotANumber) {
        printType(notANumber.type)
        write("#nan")
    }

    override fun printPositiveInfinity(positiveInfinity: KdlNumber.PositiveInfinity) {
        printType(positiveInfinity.type)
        write("#inf")
    }

    override fun printNegativeInfinity(negativeInfinity: KdlNumber.NegativeInfinity) {
        printType(negativeInfinity.type)
        write("#-inf")
    }

    override fun isValidStartOfIdentifier(c: Int): Boolean {
        return isSign(c) || c == '.'.code || isUnambiguousIdentifierChar(c)
    }
}
