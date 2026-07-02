package dev.kdl

import java.math.BigDecimal as JavaBigDecimal
import java.math.BigInteger as JavaBigInteger

actual class BigInteger(private val value: JavaBigInteger) : Number() {
	actual constructor(value: Long) : this(JavaBigInteger.valueOf(value))
	actual constructor(value: String, radix: Int) : this(JavaBigInteger(value, radix))

	actual fun toBigDecimal(): BigDecimal = BigDecimal(JavaBigDecimal(value))

	actual override fun toDouble(): Double = value.toDouble()
	actual override fun toFloat(): Float = value.toFloat()
	actual override fun toLong(): Long = value.toLong()
	actual override fun toInt(): Int = value.toInt()
	actual override fun toShort(): Short = value.toShort()
	actual override fun toByte(): Byte = value.toByte()

	actual override fun toString(): String = value.toString()
	actual override fun hashCode(): Int = value.hashCode()
	actual override fun equals(other: Any?): Boolean = other is BigInteger && value == other.value
}

actual class BigDecimal(private val value: JavaBigDecimal) : Number() {
	actual constructor(value: Double) : this(JavaBigDecimal(value))
	actual constructor(value: String) : this(JavaBigDecimal(value))

	actual fun toBigInteger(): BigInteger = BigInteger(value.toBigInteger())

	actual override fun toDouble(): Double = value.toDouble()
	actual override fun toFloat(): Float = value.toFloat()
	actual override fun toLong(): Long = value.toLong()
	actual override fun toInt(): Int = value.toInt()
	actual override fun toShort(): Short = value.toShort()
	actual override fun toByte(): Byte = value.toByte()

	actual override fun toString(): String = value.toString()
	actual override fun hashCode(): Int = value.hashCode()
	actual override fun equals(other: Any?): Boolean = other is BigDecimal && value == other.value
}
