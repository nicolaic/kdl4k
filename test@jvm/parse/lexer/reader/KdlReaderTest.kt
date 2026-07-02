package dev.kdl.parse.lexer.reader

import dev.kdl.parse.KdlInternalParseException
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals

internal class KdlReaderTest {
    @Nested
    @DisplayName("read() should")
    internal inner class Read {
        @Test
        @DisplayName("return -1 when input stream is at end")
        fun eof() {
            val reader = reader("")

            assertEquals(KdlReader.EOF, reader.read())
        }

        @Test
        @DisplayName("return 0x41 when input stream starts with 'A'")
        fun oneByteCodepoint() {
            val reader = reader("A")

            assertEquals(0x41, reader.read())
        }

        @Test
        @DisplayName("return 0xB6 when input stream starts with '¶'")
        fun twoBytesCodePoint() {
            val reader = reader("¶")

            assertEquals(0xB6, reader.read())
        }

        @Test
        @DisplayName("return 0x0801 when input stream starts with 'ࠁ'")
        fun threeBytesCodePoint() {
            val reader = reader("ࠁ")

            assertEquals(0x0801, reader.read())
        }

        @Test
        @DisplayName("return 0x10001 when input stream starts with '\uD800\uDC01'")
        fun fourBytesCodePoint() {
            val reader = reader("\uD800\uDC01")

            assertEquals(0x10001, reader.read())
        }

        @Test
        @DisplayName("throw a KdlReadException when the input stream contains an invalid 1-byte UTF-8 codepoint")
        fun invalidOneByteCodepoint() {
            val reader = reader(ByteArrayInputStream(byteArrayOf(-128)), 1)

            val exception = assertThrows<KdlReadException>(reader::read)
            assertEquals("Invalid 1-byte UTF-8 codepoint: 0x80", exception.message)
        }

        @Test
        @DisplayName("throw a KdlReadException when the input stream contains an invalid 2-bytes UTF-8 codepoints")
        fun invalidTowBytesCodepoint() {
            val reader = reader(ByteArrayInputStream(byteArrayOf(-64, 0)), 1)

            val exception = assertThrows<KdlReadException>(reader::read)
            assertEquals("Invalid 2-bytes UTF-8 codepoint: 0xC0 0x00", exception.message)
        }

        @Test
        @DisplayName("throw a KdlReadException when the input stream contains an invalid 3-bytes UTF-8 codepoints")
        fun invalidThreeBytesCodepoint() {
            val reader = reader(ByteArrayInputStream(byteArrayOf(-32, 0, 0)), 1)

            val exception = assertThrows<KdlReadException>(reader::read)
            assertEquals("Invalid 3-bytes UTF-8 codepoint: 0xE0 0x00 0x00", exception.message)
        }

        @Test
        @DisplayName("throw a KdlReadException when the input stream contains an invalid 4-bytes UTF-8 codepoints")
        fun invalidFourBytesCodepoint() {
            val reader = reader(ByteArrayInputStream(byteArrayOf(-16, 0, 0, 0)), 1)

            val exception = assertThrows<KdlReadException>(reader::read)
            assertEquals("Invalid 4-bytes UTF-8 codepoint: 0xF0 0x00 0x00 0x00", exception.message)
        }

        @Test
        @DisplayName("throw a KdlReadException when the input stream contains a UTF-8 codepoint that is invalid in a KDL document")
        fun invalidKdlCodepoint() {
            val reader = reader(ByteArrayInputStream(byteArrayOf(0)), 1)

            val exception = assertThrows<KdlReadException>(reader::read)
            assertEquals("Invalid codepoint in a KDL document: U+0000", exception.message)
        }

        @Test
        @DisplayName("return the peeked codepoint when peek has been called before read")
        fun readAfterPeek() {
            val reader = reader("A")
            val peeked = reader.peek()

            assertEquals(peeked, reader.read())
        }
    }

    @Nested
    @DisplayName("peek() should")
    internal inner class Peek {
        @Test
        @DisplayName("return -1 when input stream is at end")
        fun eof() {
            val reader = reader("")

            assertEquals(-1, reader.peek())
        }

        @Test
        @DisplayName("return 0x41 when input stream starts with 'A'")
        fun singlePeek() {
            val reader = reader("A")

            assertEquals(0x41, reader.peek())
        }

        @Test
        @DisplayName("return the same result when it is called twice")
        fun twoPeeks() {
            val reader = reader("A")
            val peeked = reader.peek()

            assertEquals(peeked, reader.peek())
        }
    }

    @Nested
    @DisplayName("peek(int) should")
    internal inner class PeekInt {
        @Test
        @DisplayName("return -1 when input stream is at end and n is 0")
        fun peek0Eof() {
            val reader = reader("", 2)

            assertEquals(-1, reader.peek(0))
        }

        @Test
        @DisplayName("return -1 when input stream is at end and n is 1")
        fun peek1Eof() {
            val reader = reader("", 2)

            assertEquals(-1, reader.peek(1))
        }

        @Test
        @DisplayName("return 0x41 when input stream starts with 'A' and n is 0")
        fun peek0OneChar() {
            val reader = reader("A", 2)

            assertEquals(0x41, reader.peek(0))
        }

        @Test
        @DisplayName("return -1 when input stream contains only 'A' and n is 1")
        fun peek1OneChar() {
            val reader = reader("A", 2)

            assertEquals(-1, reader.peek(1))
        }

        @Test
        @DisplayName("return the same result when it is called twice")
        fun twoPeeks() {
            val reader = reader("A", 2)
            val peeked = reader.peek(0)

            assertEquals(peeked, reader.peek(0))
        }

        @Test
        @DisplayName("throw a KdlInternalException when n is negative")
        fun negative() {
            val reader = reader("A", 2)

            val exception = assertThrows<KdlInternalParseException>({ reader.peek(-1) })
            assertEquals("Error while peeking: n should be between 0 and 1 included but was -1", exception.message)
        }

        @Test
        @DisplayName("throw a KdlInternalException when n is too high for reader's capacity")
        fun tooHigh() {
            val reader = reader("A", 2)

            val exception = assertThrows<KdlInternalParseException>({ reader.peek(2) })
            assertEquals("Error while peeking: n should be between 0 and 1 included but was 2", exception.message)
        }
    }

    fun reader(string: String, capacity: Int = 1): KdlReader =
        reader(ByteArrayInputStream(string.toByteArray()), capacity)

    fun reader(inputStream: java.io.InputStream, capacity: Int): KdlReader =
        KdlReader(inputStream.asSource().buffered(), capacity) { c: Int -> c <= 0x08 }
}
