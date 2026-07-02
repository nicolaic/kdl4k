package dev.kdl.parse.lexer

import dev.kdl.BigDecimal
import dev.kdl.BigInteger
import dev.kdl.parse.KdlParseException
import dev.kdl.parse.Reporter
import dev.kdl.parse.context.Span
import dev.kdl.parse.lexer.token.ByteOrderMark
import dev.kdl.parse.lexer.token.NumberToken
import dev.kdl.parse.lexer.token.Token
import dev.kdl.parse.lexer.token.Token.*
import dev.kdl.parse.lexer.token.Token.Brace.ClosingBrace
import dev.kdl.parse.lexer.token.Token.Brace.OpeningBrace
import dev.kdl.parse.lexer.token.Token.Parentheses.ClosingParentheses
import dev.kdl.parse.lexer.token.Token.Parentheses.OpeningParentheses
import kotlinx.io.Buffer
import kotlinx.io.writeString
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class Kdl2LexerTest {

    @ParameterizedTest
    @MethodSource("validTestCases")
    fun validLexerTest(input: String, expectedTokens: ArgumentsAccessor) {
        val inputBuffer = Buffer().also { it.writeString(input) }
        val lexer = Kdl2Lexer("test.kdl", inputBuffer, 1)

        for (i in 1..<expectedTokens.size()) {
            val token = lexer.read()
            assertEquals(expectedTokens.get(i), token)
        }

        assertNull(lexer.read())
    }

    @ParameterizedTest
    @MethodSource("validTestCases")
    fun peekTest(input: String) {
        val inputBuffer = Buffer().also { it.writeString(input) }

        val lexer = Kdl2Lexer("test.kdl", inputBuffer, 1)
        val peeked = lexer.peek()
        val read = lexer.read()

        assertEquals(peeked, read)
    }

    @ParameterizedTest
    @MethodSource("errorTestCases")
    fun errorLexerTest(input: String, report: String) {
        val inputBuffer = Buffer().also { it.writeString(input) }

        val lexer = Kdl2Lexer("test.kdl", inputBuffer, 1)
        val exception = assertThrows<KdlParseException> {
            var token: Token?
            do {
                token = lexer.read()
            } while (token != null)
        }

        assertEquals(report, Reporter.getReport(exception))
    }

    companion object {
        @JvmStatic
        fun validTestCases(): List<Arguments> = listOf(
            Arguments.of("", null),
            Arguments.of("\ufeff", ByteOrderMark(Span.of(1, 1))),
            Arguments.of("\n", Newline("\n", Span.of(1, 1))),
            Arguments.of("\r", Newline("\r", Span.of(1, 1))),
            Arguments.of("\r\n", Newline("\r\n", Span.of(1, 1, 1, 2))),
            Arguments.of("\u000C", Newline("\u000C", Span.of(1, 1))),
            Arguments.of(" ", NodeSpace(" ", Span.of(1, 1))),
            Arguments.of("\t", NodeSpace("\t", Span.of(1, 1))),
            Arguments.of("\t ", NodeSpace("\t ", Span.of(1, 1, 1, 2))),
            Arguments.of(
                "/* hi!\n */a",
                NodeSpace("/* hi!\n */", Span.of(1, 1, 2, 3)),
                StringToken("a", Span.of(2, 4))
            ),
            Arguments.of(
                "/* hi!\n */   /* test */",
                NodeSpace("/* hi!\n */   /* test */", Span.of(1, 1, 2, 16))
            ),
            Arguments.of(
                "  /* hi!\n */a",
                NodeSpace("  /* hi!\n */", Span.of(1, 1, 2, 3)),
                StringToken("a", Span.of(2, 4))
            ),
            Arguments.of(
                "/* hi /* there */ everyone */",
                NodeSpace("/* hi /* there */ everyone */", Span.of(1, 1, 1, 29))
            ),
            Arguments.of(
                "/* hello\n girls/boys */",
                NodeSpace("/* hello\n girls/boys */", Span.of(1, 1, 2, 14))
            ),
            Arguments.of("\\", NodeSpace("\\", Span.of(1, 1))),
            Arguments.of(" \\", NodeSpace(" \\", Span.of(1, 1, 1, 2))),
            Arguments.of("\\\n   ", NodeSpace("\\\n   ", Span.of(1, 1, 2, 3))),
            Arguments.of("\\\r\n   ", NodeSpace("\\\r\n   ", Span.of(1, 1, 2, 3))),
            Arguments.of(
                "\\   // single-line comment",
                NodeSpace("\\   // single-line comment", Span.of(1, 1, 1, 26))
            ),
            Arguments.of(
                "\\ /* multi-line\n comment */  // single-line comment",
                NodeSpace(
                    "\\ /* multi-line\n comment */  // single-line comment",
                    Span.of(1, 1, 2, 35)
                )
            ),
            Arguments.of(
                "\\   \na",
                NodeSpace("\\   \n", Span.of(1, 1, 1, 5)),
                StringToken("a", Span.of(2, 1))
            ),
            Arguments.of(
                "a  \\   \nb",
                StringToken("a", Span.of(1, 1)),
                NodeSpace("  \\   \n", Span.of(1, 2, 1, 8)),
                StringToken("b", Span.of(2, 1))
            ),
            Arguments.of(
                "//\na",
                SingleLineComment("//\n", Span.of(1, 1, 1, 3)),
                StringToken("a", Span.of(2, 1))
            ),
            Arguments.of(
                "//\r\na",
                SingleLineComment("//\r\n", Span.of(1, 1, 1, 4)),
                StringToken("a", Span.of(2, 1))
            ),
            Arguments.of("/-", Slashdash("/-", Span.of(1, 1, 1, 2))),
            Arguments.of(
                "/-   // comment\n\n  ",
                Slashdash("/-   // comment\n\n  ", Span.of(1, 1, 3, 2))
            ),
            Arguments.of(
                "/-   /* multiline\ncomment */\n\n  ",
                Slashdash("/-   /* multiline\ncomment */\n\n  ", Span.of(1, 1, 4, 2))
            ),
            Arguments.of("=", EqualsSign(Span.of(1, 1))),
            Arguments.of("(", OpeningParentheses(Span.of(1, 1))),
            Arguments.of(")", ClosingParentheses(Span.of(1, 1))),
            Arguments.of("{", OpeningBrace(Span.of(1, 1))),
            Arguments.of("}", ClosingBrace(Span.of(1, 1))),
            Arguments.of(";", Semicolon(Span.of(1, 1))),
            Arguments.of("a", StringToken("a", Span.of(1, 1))),
            Arguments.of("abc", StringToken("abc", Span.of(1, 1, 1, 3))),
            Arguments.of("-", StringToken("-", Span.of(1, 1))),
            Arguments.of("-abc", StringToken("-abc", Span.of(1, 1, 1, 4))),
            Arguments.of(
                ". ",
                StringToken(".", Span.of(1, 1)),
                NodeSpace(" ", Span.of(1, 2))
            ),
            Arguments.of(
                ".abc ",
                StringToken(".abc", Span.of(1, 1, 1, 4)),
                NodeSpace(" ", Span.of(1, 5))
            ),
            Arguments.of("+.abc", StringToken("+.abc", Span.of(1, 1, 1, 5))),
            Arguments.of("\"\"", StringToken("", Span.of(1, 1, 1, 2))),
            Arguments.of("\"abc\"", StringToken("abc", Span.of(1, 1, 1, 5))),
            Arguments.of("\"\\\"\"", StringToken("\"", Span.of(1, 1, 1, 4))),
            Arguments.of("\"\\\\\"", StringToken("\\", Span.of(1, 1, 1, 4))),
            Arguments.of("\"\\b\"", StringToken("\b", Span.of(1, 1, 1, 4))),
            Arguments.of("\"\\f\"", StringToken("\u000c", Span.of(1, 1, 1, 4))),
            Arguments.of("\"\\r\"", StringToken("\r", Span.of(1, 1, 1, 4))),
            Arguments.of("\"\\n\"", StringToken("\n", Span.of(1, 1, 1, 4))),
            Arguments.of("\"\\r\\n\"", StringToken("\r\n", Span.of(1, 1, 1, 6))),
            Arguments.of("\"\\t\"", StringToken("\t", Span.of(1, 1, 1, 4))),
            Arguments.of("\"\\s\"", StringToken(" ", Span.of(1, 1, 1, 4))),
            Arguments.of("\"\\u{1}\"", StringToken("\u0001", Span.of(1, 1, 1, 7))),
            Arguments.of("\"\\u{1234}\"", StringToken("ሴ", Span.of(1, 1, 1, 10))),
            Arguments.of("\"\\u{1F643}\"", StringToken("\uD83D\uDE43", Span.of(1, 1, 1, 11))),
            Arguments.of(
                "\"\\u{100000}\"",
                StringToken("\uDBC0\uDC00", Span.of(1, 1, 1, 12))
            ),
            Arguments.of(
                "\"a\\   \n\nb\\   c\\  \n\"",
                StringToken("abc", Span.of(1, 1, 4, 1))
            ),
            Arguments.of(
                "\"a\\   \r\n\r\nb\\   c\\  \n\"",
                StringToken("abc", Span.of(1, 1, 4, 1))
            ),
            Arguments.of(
                "\"\"\"\nHello,\nWorld!\n\"\"\"",
                StringToken("Hello,\nWorld!", Span.of(1, 1, 4, 3))
            ),
            Arguments.of(
                "\"\"\"\r\nHello,\r\nWorld!\r\n\"\"\"",
                StringToken("Hello,\nWorld!", Span.of(1, 1, 4, 3))
            ),
            Arguments.of(
                "\"\"\"\nHello,\\n\nWorld!\n\"\"\"",
                StringToken("Hello,\n\nWorld!", Span.of(1, 1, 4, 3))
            ),
            Arguments.of(
                "\"\"\"\n    Hello,\n    World!\n    \"\"\"",
                StringToken("Hello,\nWorld!", Span.of(1, 1, 4, 7))
            ),
            Arguments.of(
                "\"\"\"\n    Hello,\n\n      World!\n    \"\"\"",
                StringToken("Hello,\n\n  World!", Span.of(1, 1, 5, 7))
            ),
            Arguments.of(
                "\"\"\"\n  ab\\   c\n  \"\"\"",
                StringToken("abc", Span.of(1, 1, 3, 5))
            ),
            Arguments.of(
                "\"\"\"\n\"\"abc\"\"\n\"\"\"",
                StringToken("\"\"abc\"\"", Span.of(1, 1, 3, 3))
            ),
            Arguments.of(
                "\"\"\"\na\\\\ b\n\"\"\"",
                StringToken("a\\ b", Span.of(1, 1, 3, 3))
            ),
            Arguments.of(
                "\"\"\"\n\\\"\"\"\n\"\"\"",
                StringToken("\"\"\"", Span.of(1, 1, 3, 3))
            ),
            Arguments.of(
                "\"\"\"\n  foo \\\nbar\n  baz\n  \\   \"\"\"",
                StringToken("foo bar\nbaz", Span.of(1, 1, 5, 9))
            ),
            Arguments.of(
                "\"\"\"\n\t  \n abc\n     \n \"\"\"",
                StringToken("\nabc\n", Span.of(1, 1, 5, 4))
            ),
            Arguments.of("#\"\"abc\"\"#", StringToken("\"abc\"", Span.of(1, 1, 1, 9))),
            Arguments.of(
                "##\"Hello\\n\\r\\asd\"#world\"##",
                StringToken("Hello\\n\\r\\asd\"#world", Span.of(1, 1, 1, 26))
            ),
            Arguments.of("###\"\"#\"##\"###", StringToken("\"#\"##", Span.of(1, 1, 1, 13))),
            Arguments.of(
                "#\"\"\"\nHello,\nWorld!\n\"\"\"#",
                StringToken("Hello,\nWorld!", Span.of(1, 1, 4, 4))
            ),
            Arguments.of(
                "####\"\"\"\n   Hello,\\n\n    World!\"###\n   \"\"\"####",
                StringToken("Hello,\\n\n World!\"###", Span.of(1, 1, 4, 10))
            ),
            Arguments.of(
                "##\"\"\"\n\"\"\"abc\"\"\"\n\"\"\"##",
                StringToken("\"\"\"abc\"\"\"", Span.of(1, 1, 3, 5))
            ),
            Arguments.of("#true", BooleanToken(true, "#true", Span.of(1, 1, 1, 5))),
            Arguments.of("#false", BooleanToken(false, "#false", Span.of(1, 1, 1, 6))),
            Arguments.of("#null", Null(Span.of(1, 1, 1, 5))),
            Arguments.of("#inf", NumberToken.PositiveInfinity(Span.of(1, 1, 1, 4))),
            Arguments.of("#-inf", NumberToken.NegativeInfinity(Span.of(1, 1, 1, 5))),
            Arguments.of("#nan", NumberToken.NaN(Span.of(1, 1, 1, 4))),
            Arguments.of(
                "123",
                NumberToken.Integer(BigInteger(123), Span.of(1, 1, 1, 3))
            ),
            Arguments.of(
                "+123",
                NumberToken.Integer(BigInteger(123), Span.of(1, 1, 1, 4))
            ),
            Arguments.of(
                "-123",
                NumberToken.Integer(BigInteger(-123), Span.of(1, 1, 1, 4))
            ),
            Arguments.of(
                "-1_2_3",
                NumberToken.Integer(BigInteger(-123), Span.of(1, 1, 1, 6))
            ),
            Arguments.of("-_123", StringToken("-_123", Span.of(1, 1, 1, 5))),
            Arguments.of(
                "0x12",
                NumberToken.Integer(BigInteger(18L), Span.of(1, 1, 1, 4))
            ),
            Arguments.of(
                "0x1_2",
                NumberToken.Integer(BigInteger(18L), Span.of(1, 1, 1, 5))
            ),
            Arguments.of(
                "-0x12",
                NumberToken.Integer(BigInteger(-18L), Span.of(1, 1, 1, 5))
            ),
            Arguments.of(
                "0o12",
                NumberToken.Integer(BigInteger(10L), Span.of(1, 1, 1, 4))
            ),
            Arguments.of(
                "0o1_2",
                NumberToken.Integer(BigInteger(10L), Span.of(1, 1, 1, 5))
            ),
            Arguments.of(
                "-0o12",
                NumberToken.Integer(BigInteger(-10L), Span.of(1, 1, 1, 5))
            ),
            Arguments.of(
                "0b101",
                NumberToken.Integer(BigInteger(5L), Span.of(1, 1, 1, 5))
            ),
            Arguments.of(
                "0b1_01",
                NumberToken.Integer(BigInteger(5L), Span.of(1, 1, 1, 6))
            ),
            Arguments.of(
                "-0b101",
                NumberToken.Integer(BigInteger(-5L), Span.of(1, 1, 1, 6))
            ),
            Arguments.of("2.5", NumberToken.Decimal(BigDecimal("2.5"), Span.of(1, 1, 1, 3))),
            Arguments.of(
                "123.456",
                NumberToken.Decimal(BigDecimal("123.456"), Span.of(1, 1, 1, 7))
            ),
            Arguments.of(
                "123_456.789",
                NumberToken.Decimal(BigDecimal("123456.789"), Span.of(1, 1, 1, 11))
            ),
            Arguments.of(
                "-123.456",
                NumberToken.Decimal(BigDecimal("-123.456"), Span.of(1, 1, 1, 8))
            ),
            Arguments.of(
                "123e3",
                NumberToken.Decimal(BigDecimal("1.23E5"), Span.of(1, 1, 1, 5))
            ),
            Arguments.of(
                "1_2_3e3_",
                NumberToken.Decimal(BigDecimal("1.23E5"), Span.of(1, 1, 1, 8))
            ),
            Arguments.of(
                "-123.456e-3",
                NumberToken.Decimal(BigDecimal("-0.123456"), Span.of(1, 1, 1, 11))
            ),
            Arguments.of(
                "123 123",
                NumberToken.Integer(BigInteger(123), Span.of(1, 1, 1, 3)),
                NodeSpace(" ", Span.of(1, 4)),
                NumberToken.Integer(BigInteger(123), Span.of(1, 5, 1, 7))
            ),
            Arguments.of(
                "(type) node",
                OpeningParentheses(Span.of(1, 1)),
                StringToken("type", Span.of(1, 2, 1, 5)),
                ClosingParentheses(Span.of(1, 6)),
                NodeSpace(" ", Span.of(1, 7)),
                StringToken("node", Span.of(1, 8, 1, 11))
            ),
            Arguments.of(
                "node 0xabcdef1234567890",
                StringToken("node", Span.of(1, 1, 1, 4)),
                NodeSpace(" ", Span.of(1, 5)),
                NumberToken.Integer(BigInteger("abcdef1234567890", 16), Span.of(1, 6, 1, 23))
            ),
            Arguments.of("/**m/g/*/N*/*/", NodeSpace("/**m/g/*/N*/*/", Span.of(1, 1, 1, 14)))
        )

        @JvmStatic
        fun errorTestCases(): List<Arguments> = listOf(
            Arguments.of(
                "/* hi!",
                """
                × Unexpected end of file in multi-line comment:
                  ╭─[test.kdl:1:7]
                1 │ /* hi!
                  ·       ┬
                  ·       ╰ end of file
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "/ ",
                """
                × Unexpected character after '/':
                  ╭─[test.kdl:1:2]
                1 │ /
                  ·  ┬
                  ·  ╰ '/', '*', or '-' expected here
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "true",
                """
                × Keyword used as identifier:
                  ╭─[test.kdl:1:1]
                1 │ true
                  · ─┬──
                  ·  ╰ invalid identifier
                  ╰─
                help: for the corresponding keyword use '#' (#true), for an identifier use quotes ("true")
                """.trimIndent()
            ),
            Arguments.of(
                "\"",
                """
                × Unexpected end of file in string:
                  ╭─[test.kdl:1:2]
                1 │ "
                  ·  ┬
                  ·  ╰ end of file
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"a\n\"",
                """
                × Unexpected new line in string:
                  ╭─[test.kdl:1:3]
                1 │ "a
                  ·   ┬
                  ·   ╰ new line
                  ╰─
                help: escape it or use a multi-line string
                """.trimIndent()
            ),
            Arguments.of(
                "\"\\u1337\"",
                """
                × Unicode escape without '{':
                  ╭─[test.kdl:1:4]
                1 │ "\u1337"
                  ·    ┬
                  ·    ╰ missing '{' here
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"\\u{12g4}\"",
                """
                × Unexpected character in Unicode escape:
                  ╭─[test.kdl:1:7]
                1 │ "\u{12g4}"
                  ·       ┬
                  ·       ╰ invalid character
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"\\u{1234\"",
                """
                × Unexpected character in Unicode escape:
                  ╭─[test.kdl:1:9]
                1 │ "\u{1234"
                  ·         ┬
                  ·         ╰ invalid character
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"\\u{}\"",
                """
                × Invalid Unicode escape:
                  ╭─[test.kdl:1:2]
                1 │ "\u{}"
                  ·  ─┬──
                  ·   ╰ invalid Unicode escape
                  ╰─
                help: a unicode escape must have between 1 and 6 hexadecimal digits
                """.trimIndent()
            ),
            Arguments.of(
                "\"\\u{123",
                """
                × Unexpected end of file in Unicode escape:
                  ╭─[test.kdl:1:8]
                1 │ "\u{123
                  ·        ┬
                  ·        ╰ end of file
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"\\u{1234567}\"",
                """
                × Invalid Unicode escape:
                  ╭─[test.kdl:1:2]
                1 │ "\u{1234567}"
                  ·  ─────┬─────
                  ·       ╰ invalid Unicode escape
                  ╰─
                help: a unicode escape must have between 1 and 6 hexadecimal digits
                """.trimIndent()
            ),
            Arguments.of(
                "\"\\u{D800}\"",
                """
                × Invalid Unicode escape:
                  ╭─[test.kdl:1:5]
                1 │ "\u{D800}"
                  ·     ─┬──
                  ·      ╰ invalid Unicode scalar
                  ╰─
                help: a Unicode escape must contain a valid Unicode scalar value in hexadecimal characters
                """.trimIndent()
            ),
            Arguments.of(
                "\"\"\"\n  ab\n  cde\"\"\"",
                """
                × Unexpected character in last line of multi-line string:
                  ╭─[test.kdl:3:3]
                1 │ ""${'"'}
                2 │   ab
                3 │   cde""${'"'}
                  ·   ┬
                  ·   ╰ unexpected character
                  ╰─
                help: the last line of a multi-line string must only contain whitespaces
                """.trimIndent()
            ),
            Arguments.of(
                "#\"\"\"\n  ab\n  cde\"\"\"#",
                """
                × Unexpected character in last line of multi-line string:
                  ╭─[test.kdl:3:3]
                1 │ #""${'"'}
                2 │   ab
                3 │   cde""${'"'}#
                  ·   ┬
                  ·   ╰ unexpected character
                  ╰─
                help: the last line of a multi-line string must only contain whitespaces
                """.trimIndent()
            ),
            Arguments.of(
                "\"\"\"\n    ab\n   cd\n    \"\"\"",
                """
                × Invalid indentation in multi-line string:
                  ╭─[test.kdl:3:1]
                1 │ ""${'"'}
                2 │     ab
                3 │    cd
                  · ─┬─
                  ·  ╰ indentation does not match last line
                4 │     ""${'"'}
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "#\"\"\"\n    ab\n   cd\n    \"\"\"#",
                """
                × Invalid indentation in multi-line string:
                  ╭─[test.kdl:3:1]
                1 │ #""${'"'}
                2 │     ab
                3 │    cd
                  · ─┬─
                  ·  ╰ indentation does not match last line
                4 │     ""${'"'}#
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "##abc##",
                """
                × Raw string is missing opening quotes:
                  ╭─[test.kdl:1:3]
                1 │ ##abc##
                  ·   ┬
                  ·   ╰ missing '"'
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "#\"",
                """
                × Unexpected end of file in raw string:
                  ╭─[test.kdl:1:3]
                1 │ #"
                  ·   ┬
                  ·   ╰ end of file
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "##\"a\"#\n\"##",
                """
                × Unexpected new line in raw string:
                  ╭─[test.kdl:1:7]
                1 │ ##"a"#
                  ·       ┬
                  ·       ╰ new line
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\\ /a",
                """
                × Unexpected character after '/':
                  ╭─[test.kdl:1:4]
                1 │ \ /a
                  ·    ┬
                  ·    ╰ '/' or '*' expected here
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\\ a",
                """
                × Unexpected character in line continuation:
                  ╭─[test.kdl:1:3]
                1 │ \ a
                  ·   ┬
                  ·   ╰ unexpected character
                  ╰─
                help: a line continuation can only contain whitespaces or comments
                """.trimIndent()
            ),
            Arguments.of(
                "#abc#",
                """
                × Invalid keyword '#abc':
                  ╭─[test.kdl:1:1]
                1 │ #abc#
                  · ─┬──
                  ·  ╰ unknown keyword
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "#\"\"\"one line\"\"\"#",
                """
                × Newline required after opening quotes in multi-line raw string:
                  ╭─[test.kdl:1:5]
                1 │ #""${'"'}one line""${'"'}#
                  ·     ┬
                  ·     ╰ new-line expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "0bx01",
                """
                × Integer must start with a digit:
                  ╭─[test.kdl:1:3]
                1 │ 0bx01
                  ·   ┬
                  ·   ╰ digit expected here
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "0x1.3",
                """
                × Invalid character '.' in integer:
                  ╭─[test.kdl:1:4]
                1 │ 0x1.3
                  ·    ┬
                  ·    ╰ digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "1.",
                """
                × Invalid character in number after decimal separator:
                  ╭─[test.kdl:1:3]
                1 │ 1.
                  ·   ┬
                  ·   ╰ digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "1._",
                """
                × Invalid character '_' in number after decimal separator:
                  ╭─[test.kdl:1:3]
                1 │ 1._
                  ·   ┬
                  ·   ╰ digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "1._7",
                """
                × Invalid character '_' in number after decimal separator:
                  ╭─[test.kdl:1:3]
                1 │ 1._7
                  ·   ┬
                  ·   ╰ digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "1e",
                """
                × Invalid character in number after exponential character:
                  ╭─[test.kdl:1:3]
                1 │ 1e
                  ·   ┬
                  ·   ╰ digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "1.e7",
                """
                × Invalid character 'e' in number after decimal separator:
                  ╭─[test.kdl:1:3]
                1 │ 1.e7
                  ·   ┬
                  ·   ╰ digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "node .0n",
                """
                × Number or identifier cannot start with '.':
                  ╭─[test.kdl:1:6]
                1 │ node .0n
                  ·      ┬
                  ·      ╰ invalid character
                  ╰─
                help: for a number add a zero before '.', for an identifier use quotes
                """.trimIndent()
            ),
            Arguments.of(
                "0x",
                """
                × Integer must start with a digit:
                  ╭─[test.kdl:1:3]
                1 │ 0x
                  ·   ┬
                  ·   ╰ digit expected here
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "0x_10",
                """
                × Integer must start with a digit:
                  ╭─[test.kdl:1:3]
                1 │ 0x_10
                  ·   ┬
                  ·   ╰ digit expected here
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "0n",
                """
                × Invalid character 'n' in number:
                  ╭─[test.kdl:1:2]
                1 │ 0n
                  ·  ┬
                  ·  ╰ digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "0x10g10",
                """
                × Invalid character 'g' in integer:
                  ╭─[test.kdl:1:5]
                1 │ 0x10g10
                  ·     ┬
                  ·     ╰ digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "0b17",
                """
                × Invalid character '7' in binary integer:
                  ╭─[test.kdl:1:4]
                1 │ 0b17
                  ·    ┬
                  ·    ╰ binary digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "0o18",
                """
                × Invalid character '8' in octal integer:
                  ╭─[test.kdl:1:4]
                1 │ 0o18
                  ·    ┬
                  ·    ╰ octal digit expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "/- /-",
                """
                × Unexpected character after '/':
                  ╭─[test.kdl:1:5]
                1 │ /- /-
                  ·     ┬
                  ·     ╰ '/' or '*' expected here
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"oops",
                """
                × Unexpected end of file in string:
                  ╭─[test.kdl:1:6]
                1 │ "oops
                  ·      ┬
                  ·      ╰ end of file
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"\"\"\n  oops",
                """
                × Unexpected end of file in string:
                  ╭─[test.kdl:2:7]
                1 │ ""${'"'}
                2 │   oops
                  ·       ┬
                  ·       ╰ end of file
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"w\\ot\"",
                """
                × Invalid escaped character 'o':
                  ╭─[test.kdl:1:4]
                1 │ "w\ot"
                  ·    ┬
                  ·    ╰ '"', '\', 'b', 'f', 'r', 'n', 't', or 's' expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "#\"\"\"\n   oops",
                """
                × Unexpected end of file in raw string:
                  ╭─[test.kdl:2:8]
                1 │ #""${'"'}
                2 │    oops
                  ·        ┬
                  ·        ╰ end of file
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "abc#def",
                """
                × Invalid character '#' in identifier:
                  ╭─[test.kdl:1:4]
                1 │ abc#def
                  ·    ┬
                  ·    ╰ unexpected character
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "abc(def)",
                """
                × Invalid character '(' in identifier:
                  ╭─[test.kdl:1:4]
                1 │ abc(def)
                  ·    ┬
                  ·    ╰ unexpected character
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "\"\"\"one line\"\"\"",
                """
                × Missing newline at start of multi-line string:
                  ╭─[test.kdl:1:4]
                1 │ ""${'"'}one line""${'"'}
                  ·    ┬
                  ·    ╰ newline expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                """
                test
                ""${'"'}
                    \g
                    ""${'"'}

                """.trimIndent(),
                """
                × Invalid escaped character 'g':
                  ╭─[test.kdl:3:6]
                3 │     \g
                  ·      ┬
                  ·      ╰ '"', '\', 'b', 'f', 'r', 'n', 't', or 's' expected
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                "/*/*/",
                """
                × Unexpected end of file in multi-line comment:
                  ╭─[test.kdl:1:6]
                1 │ /*/*/
                  ·      ┬
                  ·      ╰ end of file
                  ╰─
                  """.trimIndent()
            )
        )
    }
}
