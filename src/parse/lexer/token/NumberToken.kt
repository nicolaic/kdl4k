package dev.kdl.parse.lexer.token

import dev.kdl.BigDecimal
import dev.kdl.BigInteger
import dev.kdl.KdlNumber
import dev.kdl.parse.context.Span

/**
 * Token for numbers.
 */
sealed class NumberToken(value: String, span: Span) : Token(value, span) {
    /**
     * Creates a KDL number from the current token.
     * 
     * @param type the type of the KDL number
     * @return a KDL number corresponding to this token
     */
    abstract fun asKDLNumber(type: String?): KdlNumber<*>

    /**
     * @return the value of this number token as a [BigDecimal]
     */
    abstract fun asBigDecimal(): BigDecimal?

    /**
     * Token for the positive infinity keyword.
     * 
     * @param span the span of the token
     */
    class PositiveInfinity(span: Span) : NumberToken("#inf", span) {
        override fun asKDLNumber(type: String?): KdlNumber<*> = KdlNumber.PositiveInfinity(type)
        override fun asBigDecimal(): BigDecimal = throw UnsupportedOperationException("Positive infinity cannot be converted to BigDecimal")
    }

    /**
     * Token for the negative infinity keyword.
     * 
     * @param span the span of the token
     */
    class NegativeInfinity(span: Span) : NumberToken("#-inf", span) {
        override fun asKDLNumber(type: String?): KdlNumber<*> = KdlNumber.NegativeInfinity(type)
        override fun asBigDecimal(): BigDecimal = throw UnsupportedOperationException("Negative infinity cannot be converted to BigDecimal")
    }

    /**
     * Token for the not-a-number keyword.
     * 
     * @param span the span of the token
     */
    class NaN(span: Span) : NumberToken("#nan", span) {
        override fun asKDLNumber(type: String?): KdlNumber<*> = KdlNumber.NotANumber(type)
        override fun asBigDecimal(): BigDecimal = throw UnsupportedOperationException("Not a number cannot be converted to BigDecimal")
    }

    /**
     * Token for an integer number.
     * 
     * @param span the span of the token
     */
    class Integer(private val integer: BigInteger, span: Span) : NumberToken(integer.toString(), span) {
        override fun asKDLNumber(type: String?): KdlNumber<*> = KdlNumber.Integer(integer, type)
        override fun asBigDecimal(): BigDecimal = integer.toBigDecimal()
    }

    /**
     * Token for a decimal number.
     * 
     * @param span the span of the token
     */
    class Decimal(private val decimal: BigDecimal, span: Span) : NumberToken(decimal.toString(), span) {
        override fun asKDLNumber(type: String?): KdlNumber<*> = KdlNumber.Decimal(decimal, type)
        override fun asBigDecimal(): BigDecimal = decimal
    }
}
