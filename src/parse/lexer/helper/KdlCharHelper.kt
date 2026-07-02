package dev.kdl.parse.lexer.helper

/**
 * A helper class for different predicates on codepoints.
 */
object KdlCharHelper {
    /**
     * The codepoint for the line feed character.
     */
    const val LF: Int = 0x000A

    /**
     * The codepoint for the carriage return character.
     */
    const val CR: Int = 0x000D

    /**
     * Checks if a codepoint is a valid Unicode codepoint
     * 
     * @param c a codepoint
     * @return true if c is a valid Unicode codepoint, false otherwise
     */
    fun isUnicodeScalarValue(c: Int): Boolean = c in 0..0xD7FF || c in 0xE000..0x10FFFF

    /**
     * Checks if a codepoint is a decimal digit character
     * 
     * @param c a codepoint
     * @return true if c is a decimal digit character, false otherwise
     */
    fun isDecimalDigit(c: Int): Boolean = c >= '0'.code && c <= '9'.code

    /**
     * Checks if a codepoint is a hexadecimal digit character
     * 
     * @param c a codepoint
     * @return true if c is a hexadecimal digit character, false otherwise
     */
    fun isHexadecimalDigit(c: Int): Boolean =
        isDecimalDigit(c)
                || c >= 'a'.code && c <= 'f'.code
                || c >= 'A'.code && c <= 'F'.code

    /**
     * Checks if a codepoint is an octal digit character
     * 
     * @param c a codepoint
     * @return true if c is an octal digit character, false otherwise
     */
    fun isOctalDigit(c: Int): Boolean = c >= '0'.code && c <= '7'.code

    /**
     * Checks if a codepoint is a binary digit character
     * 
     * @param c a codepoint
     * @return true if c is a binary digit character, false otherwise
     */
    fun isBinaryDigit(c: Int): Boolean = c == '0'.code || c == '1'.code

    /**
     * Checks if a codepoint is a sign character
     * 
     * @param c a codepoint
     * @return true if c is a sign character, false otherwise
     */
    fun isSign(c: Int): Boolean = c == '-'.code || c == '+'.code
}
