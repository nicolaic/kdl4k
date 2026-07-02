package dev.kdl.parse.lexer.helper

/**
 * A helper for parsing number. Represents a base for integer representation.
 */
enum class IntegerBase(
    /**
     * @return the name of this integer base
     */
    val baseName: String,

    /**
     * The radix to use when parsing a number with this integer base.
     */
    val radix: Int,

    /**
     * @return the prefix to use when printing a number with this integer base
     */
    val prefix: String,
) {
    /**
     * Base 2
     */
    BINARY("binary", 2, "0b"),

    /**
     * Base 8
     */
    OCTAL("octal", 8, "0o"),

    /**
     * Base 10
     */
    DECIMAL("decimal", 10, ""),

    /**
     * Base  16
     */
    HEXADECIMAL("hexadecimal", 16, "0x"),;

    /**
     * A predicate that can check if a codepoint is a valid digit for the current integer base.
     * 
     * @return a predicate for valid digits in this integer base
     */
    fun predicate(): (Int) -> Boolean = when (this) {
        BINARY -> KdlCharHelper::isBinaryDigit
        OCTAL -> KdlCharHelper::isOctalDigit
        DECIMAL -> KdlCharHelper::isDecimalDigit
        HEXADECIMAL -> KdlCharHelper::isHexadecimalDigit
    }
}
