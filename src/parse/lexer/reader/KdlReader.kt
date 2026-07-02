package dev.kdl.parse.lexer.reader

import dev.kdl.parse.KdlInternalParseException
import dev.kdl.parse.lexer.reader.KdlReader.Companion.EOF
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlin.jvm.JvmOverloads

/**
 * A reader for valid Unicode codepoints, with peeking capabilities.
 */
class KdlReader(
    private val source: Source,
    capacity: Int,
    private val invalidCodepoint: InvalidCodepoint,
) {
    private val peekedChars: IntRingBuffer = IntRingBuffer(capacity)

    /**
     * Peeks the next codepoint.
     *
     * @return the next codepoint, or [.EOF] if the end of the stream has been reached
     * @throws IOException when there is an error reading the stream
     */
    /**
     * Peeks a codepoint in the stream. It is 0-based, therefore `peek(0)` is the same as `peek()`.
     *
     * @param n the index of the codepoint to peek
     * @return the nth codepoint,  or [.EOF] if the end of the stream has been reached
     * @throws IOException when there is an error reading the stream
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun peek(n: Int = 0): Int {
        if (n < 0 || n >= peekedChars.capacity()) {
            throw KdlInternalParseException("Error while peeking: n should be between 0 and " + (peekedChars.capacity() - 1) + " included but was " + n)
        }

        while (peekedChars.size() <= n) {
            val c = readNextChar()
            if (c == EOF) {
                return EOF
            }
            peekedChars.addLast(c)
        }

        return peekedChars.get(n)
    }

    /**
     * Reads the next token and advances the reader.
     *
     * @return the next codepoint, or [.EOF] if the end of the stream has been reached
     * @throws IOException when there is an error reading the stream
     */
    @Throws(IOException::class)
    fun read(): Int {
        return if (peekedChars.isEmpty) readNextChar() else peekedChars.removeFirst()
    }

    @Throws(IOException::class)
    private fun readNextChar(): Int {
        val codepoint = readUtf8Codepoint()
        if (codepoint != EOF && invalidCodepoint.isInvalid(codepoint)) {
            throw KdlReadException("Invalid codepoint in a KDL document: U+${codepoint.formatHex(4)}")
        }
        return codepoint
    }

    @Throws(IOException::class)
    private fun readUtf8Codepoint(): Int {
        val c = source.read()
        when {
            c == EOF -> return EOF
            (c and 128) == 0 -> {
                // 1-byte codepoint
                return c
            }

            (c and 224) == 192 -> {
                // 2-bytes codepoint
                val c2 = source.read()
                if (isInvalidExtraUtf8Byte(c2)) {
                    throw KdlReadException(
                        "Invalid 2-bytes UTF-8 codepoint: " +
                                "0x${c.formatHex(2)} " +
                                "0x${c2.formatHex(2)}"
                    )
                }
                return (c and 31) shl 6 or (c2 and 63)
            }

            (c and 240) == 224 -> {
                // 3-bytes codepoint
                val c2 = source.read()
                val c3 = source.read()
                if (isInvalidExtraUtf8Byte(c2) || isInvalidExtraUtf8Byte(c3)) {
                    throw KdlReadException(
                        "Invalid 3-bytes UTF-8 codepoint: " +
                                "0x${c.formatHex(2)} " +
                                "0x${c2.formatHex(2)} " +
                                "0x${c3.formatHex(2)}"
                    )
                }
                return (c and 15) shl 12 or ((c2 and 63) shl 6) or (c3 and 63)
            }

            (c and 248) == 240 -> {
                // 4-bytes codepoint
                val c2 = source.read()
                val c3 = source.read()
                val c4 = source.read()
                if (isInvalidExtraUtf8Byte(c2) || isInvalidExtraUtf8Byte(c3) || isInvalidExtraUtf8Byte(c4)) {
                    throw KdlReadException(
                        "Invalid 4-bytes UTF-8 codepoint: " +
                                "0x${c.formatHex(2)} " +
                                "0x${c2.formatHex(2)} " +
                                "0x${c3.formatHex(2)} " +
                                "0x${c4.formatHex(2)}"
                    )
                }
                return (c and 7) shl 18 or ((c2 and 63) shl 12) or ((c3 and 63) shl 6) or (c4 and 63)
            }

            else -> throw KdlReadException("Invalid 1-byte UTF-8 codepoint: 0x${c.formatHex(2)}")
        }
    }

    private fun isInvalidExtraUtf8Byte(b: Int): Boolean {
        return (b and 192) != 128
    }

    /**
     * Predicate that checks if a codepoint is invalid.
     */
    fun interface InvalidCodepoint {
        /**
         * Checks if a codepoint is invalid.
         *
         * @param codepoint the codepoint to check
         * @return `true` if the codepoint is invalid, `false` otherwise
         */
        fun isInvalid(codepoint: Int): Boolean
    }

    companion object {
        /**
         * The value returned to symbolize the end of the file.
         */
        val EOF: Int = -1
    }
}

private fun Source.read(): Int =
    if (exhausted()) EOF else readByte().toInt()

private fun Int.formatHex(padding: Int): String =
    toHexString(HexFormat.UpperCase).let {
		it.substring(it.length - padding).padStart(padding, '0')
	}
