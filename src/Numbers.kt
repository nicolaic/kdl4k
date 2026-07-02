package dev.kdl

expect class BigInteger : Number {
    constructor(value: Long = 0)
    constructor(value: String, radix: Int = 10)

    fun toBigDecimal(): BigDecimal

    override fun toDouble(): Double
    override fun toFloat(): Float
    override fun toLong(): Long
    override fun toInt(): Int
    override fun toShort(): Short
    override fun toByte(): Byte

	override fun toString(): String
	override fun hashCode(): Int
	override fun equals(other: Any?): Boolean
}

expect class BigDecimal : Number {
    constructor(value: Double = 0.0)
    constructor(value: String)

    fun toBigInteger(): BigInteger

    override fun toDouble(): Double
    override fun toFloat(): Float
    override fun toLong(): Long
    override fun toInt(): Int
    override fun toShort(): Short
    override fun toByte(): Byte

	override fun toString(): String
	override fun hashCode(): Int
	override fun equals(other: Any?): Boolean
}
