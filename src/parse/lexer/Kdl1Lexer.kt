package dev.kdl.parse.lexer

import dev.kdl.BigDecimal
import dev.kdl.BigInteger
import dev.kdl.parse.KdlParseException
import dev.kdl.parse.context.Position
import dev.kdl.parse.context.Span
import dev.kdl.parse.lexer.helper.IntegerBase
import dev.kdl.parse.lexer.helper.Kdl1CharHelper.isIdentifierChar
import dev.kdl.parse.lexer.helper.Kdl1CharHelper.isNewline
import dev.kdl.parse.lexer.helper.Kdl1CharHelper.isWhitespace
import dev.kdl.parse.lexer.helper.KdlCharHelper
import dev.kdl.parse.lexer.helper.KdlCharHelper.CR
import dev.kdl.parse.lexer.helper.KdlCharHelper.LF
import dev.kdl.parse.lexer.helper.KdlCharHelper.isDecimalDigit
import dev.kdl.parse.lexer.helper.KdlCharHelper.isHexadecimalDigit
import dev.kdl.parse.lexer.helper.KdlCharHelper.isSign
import dev.kdl.parse.lexer.helper.KdlCharHelper.isUnicodeScalarValue
import dev.kdl.parse.lexer.reader.KdlReader
import dev.kdl.parse.lexer.reader.KdlReader.Companion.EOF
import dev.kdl.parse.lexer.token.NumberToken
import dev.kdl.parse.lexer.token.Token
import dev.kdl.parse.lexer.token.Token.*
import kotlinx.io.*

/**
 * Lexer for the KDL 1.0 syntax.
 *
 * @param filename the name of the file bien parsed
 * @param source   the source to parse
 * @param capacity the maximum number of token that can be peeked ahead
 */
class Kdl1Lexer(
    filename: String?,
    source: Source,
    capacity: Int,
) : AbstractKdlLexer(
    filename,
    KdlReader(source, READER_CAPACITY, { c -> c <= 0x08 }),
    capacity,
) {
    @Throws(IOException::class, KdlParseException::class)
    override fun nextToken(): Token? {
        val c = peekChar()

        when (c) {
            EOF -> return null
            '='.code -> return consumeAndCreate<Token>(::EqualsSign)
            '('.code -> return consumeAndCreate<Token>(Parentheses::OpeningParentheses)
            ')'.code -> return consumeAndCreate<Token>(Parentheses::ClosingParentheses)
            '{'.code -> return consumeAndCreate<Token>(Brace::OpeningBrace)
            '}'.code -> return consumeAndCreate<Token>(Brace::ClosingBrace)
            ';'.code -> return consumeAndCreate<Token>(::Semicolon)
            '/'.code -> {
                val second = peekChar(1)
                return when (second) {
                    '/'.code -> singleLineComment()
                    '*'.code -> whitespace()
                    '-'.code -> slashdash()
                    else -> {
                        consumeChar()
                        throw KdlParseException(
                            "Unexpected character after '/'",
                            getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                            "'/', '*', or '-' expected here"
                        )
                    }
                }
            }

            '"'.code -> return quotedString()
            'r'.code -> {
                val second = peekChar(1)
                return if (second == '"'.code || second == '#'.code)
                    rawString() else bareIdentifier()
            }

            '\\'.code -> return lineContinuation()
        }

        if (isNewline(c)) {
            return newline()
        } else if (isWhitespace(c)) {
            return whitespace()
        } else if (isDecimalDigit(c) || isSign(c) && isDecimalDigit(peekChar(1))) {
            return number()
        } else if (this.isBareIdentifier) {
            return bareIdentifier()
        }

        throw KdlParseException(
            "Invalid character",
            getErrorParseContext(Span.of(sourceLines.getNextPosition())),
            "invalid character"
        )
    }

    @Throws(IOException::class)
    private fun newline(): Newline {
        val buffer = Buffer()
        val span: Span = newline(buffer)
        return Newline(buffer.readString(), span)
    }

    @Throws(IOException::class)
    private fun newline(buffer: Buffer): Span {
        val start = sourceLines.getNextPosition()

        val newline = readChar()
		buffer.writeCodePointValue(newline)
        if (newline == CR && peekChar() == LF) {
			buffer.writeCodePointValue(readChar())
        }

        sourceLines.newline()
        val end =
            sourceLines.getCurrentPosition()

        return Span(start, end)
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun whitespace(): Whitespace {
        val buffer = Buffer()
        val start = sourceLines.getNextPosition()

        whitespace(buffer)

        return Whitespace(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun whitespace(buffer: Buffer): Boolean {
        var hasReadCharacters = false

        while (true) {
            val c = peekChar()

            if (isWhitespace(c)) {
                hasReadCharacters = true
                buffer.writeCodePointValue(readChar())
            } else if (c == '/'.code && peekChar(1) == '*'.code) {
                hasReadCharacters = true
                multilineComment(buffer)
            } else {
                break
            }
        }

        return hasReadCharacters
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun lineContinuation(): LineContinuation {
        val buffer = Buffer()
        val start = sourceLines.getNextPosition()

        lineContinuation(buffer)

        return LineContinuation(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun lineContinuation(buffer: Buffer) {
        buffer.writeCodePointValue(readChar())

        while (true) {
            val c = peekChar()
            if (isWhitespace(c)) {
                buffer.writeCodePointValue(readChar())
            } else if (c == '/'.code) {
                val second = peekChar(1)
                if (second == '/'.code) {
                    singleLineComment(buffer)
                    break
                } else if (second == '*'.code) {
                    multilineComment(buffer)
                } else {
                    consumeChar()
                    throw KdlParseException(
                        "Unexpected character after '/'",
                        getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                        "'/' or '*' expected here"
                    )
                }
            } else if (isNewline(c)) {
                buffer.writeCodePointValue(readChar())
                if (c == CR && peekChar() == LF) {
                    buffer.writeCodePointValue(readChar())
                }
                sourceLines.newline()
                break
            } else if (c == EOF) {
                break
            } else {
                throw KdlParseException(
                    "Unexpected character in line continuation",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "unexpected character",
                    "a line continuation can only contain whitespaces or comments"
                )
            }
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun multilineComment(buffer: Buffer) {
        consumeChar(2)
        buffer.writeString("/*")

        var expectedEnds = 1

        while (true) {
            val c = peekChar()
            if (c == EOF) {
                throw KdlParseException(
                    "Unexpected end of file in multi-line comment",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "end of file"
                )
            } else if (isNewline(c)) {
                newline(buffer)
            } else {
                consumeChar()
                buffer.writeCodePointValue(c)
            }
            if (c == '/'.code && peekChar() == '*'.code) {
                consumeChar()
                buffer.writeCodePointValue('*'.code)
                expectedEnds += 1
            }
            if (c == '*'.code && peekChar() == '/'.code) {
                consumeChar()
                buffer.writeCodePointValue('/'.code)
                expectedEnds -= 1
                if (expectedEnds == 0) {
                    break
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun singleLineComment(): SingleLineComment {
        val buffer = Buffer()
        val start =
            sourceLines.getNextPosition()

        singleLineComment(buffer)

        return SingleLineComment(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
    }

    @Throws(IOException::class)
    private fun singleLineComment(buffer: Buffer) {
        consumeChar(2)
        buffer.writeString("//")

        while (true) {
            val c = peekChar()
            if (isNewline(c)) {
                newline(buffer)
                break
            } else if (c == EOF) {
                break
            }
            buffer.writeCodePointValue(readChar())
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun slashdash(): Slashdash {
        val buffer = Buffer().also { it.writeString("/-") }
        val start = sourceLines.getNextPosition()
        consumeChar(2)

        while (true) {
            if (!whitespace(buffer)) {
                if (peekChar() == '\\'.code) {
                    lineContinuation(buffer)
                } else {
                    break
                }
            }
        }

        return Slashdash(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun quotedString(): StringToken {
        val buffer = Buffer()
        val start = sourceLines.getNextPosition()
        consumeChar()

        while (true) {
            val c = peekChar()
            if (c == EOF) {
                throw KdlParseException(
                    "Unexpected end of file in string",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "end of file"
                )
            } else if (c == '"'.code) {
                consumeChar()
                break
            } else {
                consumeChar()
                if (c == '\\'.code) {
                    escapedCharacter(buffer)
                } else {
                    buffer.writeCodePointValue(c)
                    if (isNewline(c)) {
                        sourceLines.newline()
                    }
                }
            }
        }

        return StringToken(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun escapedCharacter(buffer: Buffer) {
        val c = peekChar()
        val escapedCharacter = ESCAPED_CHARACTERS[c]
        if (escapedCharacter != null) {
            consumeChar()
            buffer.writeCodePointValue(escapedCharacter)
        } else if (c == 'u'.code) {
            consumeChar()
            buffer.writeCodePointValue(unicodeEscape())
        } else {
            throw KdlParseException(
                "Invalid escaped character '" + c.toChar() + "'",
                getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                "'\"', '\\', 'b', 'f', 'r', 'n', 't', or 's' expected"
            )
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun unicodeEscape(): Int {
        if (readChar() != '{'.code) {
            throw KdlParseException(
                "Unicode escape without '{'",
                getErrorParseContext(Span.of(sourceLines.getCurrentPosition())),
                "missing '{' here"
            )
        }

        val hexBuffer = Buffer()
        while (true) {
            val c = readChar()
            if (c == '}'.code) {
                break
            } else if (isHexadecimalDigit(c)) {
				hexBuffer.writeCodePointValue(c)
            } else if (c == EOF) {
                throw KdlParseException(
                    "Unexpected end of file in Unicode escape",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "end of file"
                )
            } else {
                throw KdlParseException(
                    "Unexpected character in Unicode escape",
                    getErrorParseContext(Span.of(sourceLines.getCurrentPosition())),
                    "invalid character"
                )
            }
        }

		val hexString = hexBuffer.readString()

        if (hexString.isEmpty() || hexString.length > 6) {
            val end = sourceLines.getCurrentPosition()
            val start = end.withColumnOffset(-hexString.length - 3)
            throw KdlParseException(
                "Invalid Unicode escape",
                getErrorParseContext(Span(start, end)),
                "invalid Unicode escape",
                "a unicode escape must have between 1 and 6 hexadecimal digits"
            )
        }

		val hexValue = hexString.toInt(16)
        if (!isUnicodeScalarValue(hexValue)) {
            val currentPosition = sourceLines.getCurrentPosition()
			// TODO: Curious if this bug would get caught if not +1 to size
            val start = currentPosition.withColumnOffset(-hexString.length)
            val end = currentPosition.withColumnOffset(-1)
            throw KdlParseException(
                "Invalid Unicode escape",
                getErrorParseContext(Span(start, end)),
                "invalid Unicode scalar",
                "a Unicode escape must contain a valid Unicode scalar value in hexadecimal characters"
            )
        }

        return hexValue
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun rawString(): StringToken {
        val buffer = Buffer()
        val start = sourceLines.getNextPosition()

        consumeChar()

        var openingSharpSigns = 0
        while (peekChar() == '#'.code) {
            consumeChar()
            openingSharpSigns += 1
        }

        if (readChar() != '"'.code) {
            throw KdlParseException(
                "Raw string is missing opening quotes",
                getErrorParseContext(Span.of(sourceLines.getCurrentPosition())),
                "missing '\"'"
            )
        }

        while (true) {
            val c = peekChar()
            if (c == EOF) {
                throw KdlParseException(
                    "Unexpected end of file in raw string",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "end of file"
                )
            } else if (c == '"'.code) {
                consumeChar()
                var closingSharpSigns = 0
                while (peekChar() == '#'.code && closingSharpSigns < openingSharpSigns) {
                    consumeChar()
                    closingSharpSigns += 1
                }
                if (closingSharpSigns == openingSharpSigns) {
                    return StringToken(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
                }
                buffer.writeCodePointValue('"'.code)
                writeNonClosingSharpSigns(buffer, closingSharpSigns)
            } else {
                consumeChar()
                buffer.writeCodePointValue(c)
                if (isNewline(c)) {
                    sourceLines.newline()
                }
            }
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun number(): NumberToken {
        val start = sourceLines.getNextPosition()
        val buffer = Buffer()

        if (isSign(peekChar())) {
            buffer.writeCodePointValue(readChar())
        }

        if (peekChar() == '0'.code) {
            val peek = peekChar(1)

            when (peek) {
                'b'.code -> return integer(start, buffer, IntegerBase.BINARY)
                'o'.code -> return integer(start, buffer, IntegerBase.OCTAL)
                'x'.code -> return integer(start, buffer, IntegerBase.HEXADECIMAL)
            }
        }

        var isDecimal = false
        parseDigits(buffer)

        if (peekChar() == '.'.code) {
            consumeChar()
            isDecimal = true
            buffer.writeCodePointValue('.'.code)
            checkNextCharacter(KdlCharHelper::isDecimalDigit, "number after decimal separator")
            parseDigits(buffer)
        }

        if ((peekChar() == 'e'.code) or (peekChar() == 'E'.code)) {
            isDecimal = true
            buffer.writeCodePointValue(readChar())
            if (isSign(peekChar())) {
                buffer.writeCodePointValue(readChar())
            }
            checkNextCharacter(KdlCharHelper::isDecimalDigit, "number after exponential character")
            parseDigits(buffer)
        }

        val span = Span(start, sourceLines.getCurrentPosition())

        checkNextCharacter({ !isIdentifierChar(it) }, "number")

        return if (isDecimal) decimal(span, buffer.readString())
        else integer(span, buffer.readString(), IntegerBase.DECIMAL)
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun integer(
        start: Position,
        buffer: Buffer,
        base: IntegerBase
    ): NumberToken.Integer {
        consumeChar(2)

        if (!base.predicate()(peekChar())) {
            throw KdlParseException(
                "Integer must start with a digit",
                getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                "digit expected here"
            )
        }

        parseDigits(buffer, base.predicate())
        val span = Span(start, sourceLines.getCurrentPosition())
        val number = buffer.readString()

        checkNextCharacter({ !isHexadecimalDigit(it) }, base)
        checkNextCharacter({ !isIdentifierChar(it) }, "integer")

        return integer(span, number, base)
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun checkNextCharacter(predicate: (Int) -> Boolean, base: IntegerBase) {
        checkNextCharacter(predicate, base.baseName + " integer", base.baseName + " digit expected")
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun checkNextCharacter(predicate: (Int) -> Boolean, position: String, label: String = "digit expected") {
        val nextChar = peekChar()
        if (!predicate(nextChar)) {
            throw KdlParseException(
                (if (nextChar == EOF) "Invalid character in " else "Invalid character '${nextChar.toChar()}' in ") + position,
                getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                label
            )
        }
    }

    @Throws(IOException::class)
    private fun parseDigits(
        buffer: Buffer,
        digitPredicate: (Int) -> Boolean = KdlCharHelper::isDecimalDigit,
    ) {
        while (true) {
            val c = peekChar()
            if (digitPredicate(c)) {
                buffer.writeCodePointValue(readChar())
            } else if (c == '_'.code) {
                consumeChar()
            } else {
                break
            }
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun integer(span: Span, number: String, base: IntegerBase): NumberToken.Integer {
        try {
            return NumberToken.Integer(BigInteger(number, base.radix), span)
        } catch (_: NumberFormatException) {
            throw invalidNumber(span, number, base)
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun decimal(span: Span, number: String): NumberToken.Decimal {
        try {
            return NumberToken.Decimal(BigDecimal(number), span)
        } catch (_: NumberFormatException) {
            throw invalidNumber(span, number, IntegerBase.DECIMAL)
        }
    }

    @Throws(IOException::class)
    private fun invalidNumber(span: Span, number: String, base: IntegerBase): KdlParseException = KdlParseException(
        "Invalid number " + base.prefix + number,
        getErrorParseContext(span),
        "invalid number"
    )

    @get:Throws(IOException::class)
    private val isBareIdentifier: Boolean
        get() {
            val c = peekChar()
            if (isSign(c)) return !isDecimalDigit(peekChar(1))
            return !isDecimalDigit(c) && isIdentifierChar(c)
        }

    @Throws(IOException::class, KdlParseException::class)
    private fun bareIdentifier(): Token {
        val buffer = Buffer()
        val start = sourceLines.getNextPosition()

        while (isIdentifierChar(peekChar())) {
            buffer.writeCodePointValue(readChar())
        }

        val next = peekChar()
        if (next == '"'.code || next == '('.code || next == '['.code) {
            throw KdlParseException(
                "Invalid character '${next.toChar()}' in identifier",
                getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                "unexpected character"
            )
        }

        val span = Span(start, sourceLines.getCurrentPosition())

		return when (val value = buffer.readString()) {
            "true" -> BooleanToken(true, "true", span)
            "false" -> BooleanToken(false, "false", span)
            "null" -> Null(span)
            else -> BareIdentifier(value, span)
        }
    }

    companion object {
        private fun writeNonClosingSharpSigns(buffer: Buffer, closingSharpSigns: Int) {
            if (closingSharpSigns > 0) buffer.writeString("#".repeat(closingSharpSigns))
        }

        private const val READER_CAPACITY = 2

        private val ESCAPED_CHARACTERS: Map<Int, Int> = mapOf(
            'n'.code to '\n'.code,
            'r'.code to '\r'.code,
            't'.code to '\t'.code,
            '\\'.code to '\\'.code,
            '/'.code to '/'.code,
            '"'.code to '"'.code,
            'b'.code to '\b'.code,
            'f'.code to '\u000c'.code,
        )
    }
}
