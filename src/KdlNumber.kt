package dev.kdl

/**
 * Supertype for all KDL number values.
 *
 * @param <T> the inner representation of the number value
</T> */
sealed class KdlNumber<T : Number>(
    override val value: T,
) : KdlValue<T> {
    /**
     * @return the number converted to a byte
     */
    open fun asByte(): Byte = value.toByte()

    /**
     * @return the number converted to an int
     */
    open fun asInt(): Int = value.toInt()

    /**
     * @return the number converted to a long
     */
    open fun asLong(): Long = value.toLong()

    /**
     * @return the number converted to a [BigInteger]
     */
    abstract fun asBigInteger(): BigInteger?

    /**
     * @return the number converted to a double
     */
    open fun asDouble(): Double = value.toDouble()

    /**
     * @return the number converted to a [BigDecimal]
     */
    abstract fun asBigDecimal(): BigDecimal?

    override val isNumber: Boolean get() = true

    /**
     * The KDL Not-a-number value.
     *
     * @param type the type of the value
     */
    class NotANumber(override val type: String?) : KdlNumber<Double>(Double.NaN) {
        override fun asByte(): Nothing = throw UnsupportedOperationException("Not a number cannot be converted to byte")
        override fun asInt(): Nothing = throw UnsupportedOperationException("Not a number cannot be converted to int")
        override fun asLong(): Nothing = throw UnsupportedOperationException("Not a number cannot be converted to long")
        override fun asBigInteger(): Nothing =
            throw UnsupportedOperationException("Not a number cannot be converted to BigInteger")

        override fun asBigDecimal(): Nothing =
            throw UnsupportedOperationException("Not a number cannot be converted to BigDecimal")

        override fun asDouble(): Double = value
        override fun toString(): String = "NaN"
    }

    /**
     * The KDL positive infinity value.
     *
     * @param type the type of the value
     */
    class PositiveInfinity(override val type: String?) : KdlNumber<Double>(Double.POSITIVE_INFINITY) {
        override fun asByte(): Nothing =
            throw UnsupportedOperationException("Positive infinity cannot be converted to byte")

        override fun asInt(): Nothing =
            throw UnsupportedOperationException("Positive infinity cannot be converted to int")

        override fun asLong(): Nothing =
            throw UnsupportedOperationException("Positive infinity cannot be converted to long")

        override fun asBigInteger(): Nothing =
            throw UnsupportedOperationException("Positive infinity cannot be converted to BigInteger")

        override fun asBigDecimal(): Nothing =
            throw UnsupportedOperationException("Positive infinity cannot be converted to BigDecimal")

        override fun asDouble(): Double = value
        override fun toString(): String = "+inf"
    }

    /**
     * The negative infinity KDL value.
     *
     * @param type the type of the value
     */
    class NegativeInfinity(override val type: String?) : KdlNumber<Double>(Double.NEGATIVE_INFINITY) {

        override fun asByte(): Nothing =
            throw UnsupportedOperationException("Negative infinity cannot be converted to byte")

        override fun asInt(): Nothing =
            throw UnsupportedOperationException("Negative infinity cannot be converted to int")

        override fun asLong(): Nothing =
            throw UnsupportedOperationException("Negative infinity cannot be converted to long")

        override fun asBigInteger(): Nothing =
            throw UnsupportedOperationException("Negative infinity cannot be converted to BigInteger")

        override fun asBigDecimal(): Nothing =
            throw UnsupportedOperationException("Negative infinity cannot be converted to BigDecimal")

        override fun asDouble(): Double = value
        override fun toString(): String = "-inf"
    }

    /**
     * A KDL number representing an integer.
     *
     * @param type  the type of the value
     * @param value the integer value
     */
    data class Integer(override val value: BigInteger, override val type: String?) : KdlNumber<BigInteger>(value) {
        override fun asBigInteger(): BigInteger = value
        override fun asBigDecimal(): BigDecimal = value.toBigDecimal()

        override fun toString(): String = "Integer($value)"
    }

    /**
     * A KDL number representing a decimal number.
     *
     * @param type  the type of the value
     * @param value the integer value
     */
    data class Decimal(override val value: BigDecimal, override val type: String?) : KdlNumber<BigDecimal>(value) {
        override fun asBigInteger(): BigInteger = value.toBigInteger()
        override fun asBigDecimal(): BigDecimal = value

        override fun toString(): String = "Decimal($value)"
    }

    companion object {
        /**
         * Creates a [KdlNumber] from a [Number].
         *
         * @param number the number to represent
         * @return the corresponding [KdlNumber]
         */
        fun from(number: Number): KdlNumber<*> = from(number, null)

        /**
         * Creates a [KdlNumber] from a [Number].
         *
         * @param type   the type of the KDL number
         * @param number the number to represent
         * @return the corresponding [KdlNumber]
         */
        fun from(number: Number, type: String?): KdlNumber<*> = when (number) {
            is BigInteger -> Integer(number, type)
            is BigDecimal -> Decimal(number, type)
            is Byte, is Short, is Int, is Long -> Integer(BigInteger(number.toLong()), type)
            else -> Decimal(BigDecimal(number.toString()), type)
        }
    }
}
