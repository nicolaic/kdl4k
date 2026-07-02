package dev.kdl.parse.lexer

import dev.kdl.BigDecimal
import dev.kdl.BigInteger
import dev.kdl.parse.KdlParseException
import dev.kdl.parse.context.Position
import dev.kdl.parse.context.Span
import dev.kdl.parse.lexer.helper.IntegerBase
import dev.kdl.parse.lexer.helper.Kdl2CharHelper.isDisallowedIdentifier
import dev.kdl.parse.lexer.helper.Kdl2CharHelper.isIdentifierChar
import dev.kdl.parse.lexer.helper.Kdl2CharHelper.isNewline
import dev.kdl.parse.lexer.helper.Kdl2CharHelper.isUnambiguousIdentifierChar
import dev.kdl.parse.lexer.helper.Kdl2CharHelper.isWhitespace
import dev.kdl.parse.lexer.helper.KdlCharHelper
import dev.kdl.parse.lexer.helper.KdlCharHelper.CR
import dev.kdl.parse.lexer.helper.KdlCharHelper.LF
import dev.kdl.parse.lexer.helper.KdlCharHelper.isDecimalDigit
import dev.kdl.parse.lexer.helper.KdlCharHelper.isHexadecimalDigit
import dev.kdl.parse.lexer.helper.KdlCharHelper.isSign
import dev.kdl.parse.lexer.helper.KdlCharHelper.isUnicodeScalarValue
import dev.kdl.parse.lexer.reader.KdlReader
import dev.kdl.parse.lexer.reader.KdlReader.Companion.EOF
import dev.kdl.parse.lexer.token.ByteOrderMark
import dev.kdl.parse.lexer.token.NumberToken
import dev.kdl.parse.lexer.token.Token
import dev.kdl.parse.lexer.token.Token.*
import dev.kdl.parse.lexer.token.Token.Brace.ClosingBrace
import dev.kdl.parse.lexer.token.Token.Brace.OpeningBrace
import dev.kdl.parse.lexer.token.Token.Parentheses.ClosingParentheses
import dev.kdl.parse.lexer.token.Token.Parentheses.OpeningParentheses
import kotlinx.io.*
import kotlin.math.max

/**
 * Lexer for the KDL 2.0 syntax.
 *
 * @param filename  the name of the file being parsed
 * @param source    the source to parse
 * @param capacity  the maximum number of token that can be peeked ahead
 */
class Kdl2Lexer(
    filename: String?,
    source: Source,
    capacity: Int,
) : AbstractKdlLexer(
    filename,
    KdlReader(source, READER_CAPACITY, { codepoint: Int -> isInvalid(codepoint) }),
    capacity
) {
    @Throws(IOException::class, KdlParseException::class)
    override fun nextToken(): Token? {
        val c = peekChar()

        when (c) {
            EOF -> return null
            ByteOrderMark.VALUE.code -> return consumeAndCreate(::ByteOrderMark)
            '='.code -> return consumeAndCreate(::EqualsSign)
            '('.code -> return consumeAndCreate(::OpeningParentheses)
            ')'.code -> return consumeAndCreate(::ClosingParentheses)
            '{'.code -> return consumeAndCreate(::OpeningBrace)
            '}'.code -> return consumeAndCreate(::ClosingBrace)
            ';'.code -> return consumeAndCreate(::Semicolon)
            '/'.code -> {
                val second = peekChar(1)
                if (second == '/'.code) {
                    return singleLineComment()
                } else if (second == '*'.code) {
                    return nodeSpace()
                } else if (second == '-'.code) {
                    return slashdash()
                }
                consumeChar()
                throw KdlParseException(
                    "Unexpected character after '/'",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "'/', '*', or '-' expected here"
                )
            }

            '"'.code -> return quotedString()
            '#'.code -> {
                val second = peekChar(1)
                return if (second == '#'.code || second == '"'.code) rawString() else keyword()
            }

            '\\'.code -> return nodeSpace()
        }

        if (isNewline(c)) {
            return newline()
        } else if (isWhitespace(c)) {
            return nodeSpace()
        } else if (isDecimalDigit(c) || isSign(c) && isDecimalDigit(peekChar(1))) {
            return number()
        } else if (this.isIdentifierString) {
            return identifierString()
        }

        if (c == '.'.code) {
            throw KdlParseException(
                "Number or identifier cannot start with '.'",
                getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                "invalid character",
                "for a number add a zero before '.', for an identifier use quotes"
            )
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
        val end = sourceLines.getCurrentPosition()

        return Span(start, end)
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun nodeSpace(): NodeSpace {
        val buffer = Buffer()
        val start = sourceLines.getNextPosition()

        nodeSpace(buffer)

        return NodeSpace(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun nodeSpace(buffer: Buffer): Boolean {
        var hasReadCharacters = false

        while (true) {
            val c = peekChar()

            if (isWhitespace(c)) {
                hasReadCharacters = true
                buffer.writeCodePointValue(readChar())
            } else if (c == '\\'.code) {
                hasReadCharacters = true
                lineContinuation(buffer)
            } else if (c == '/'.code && peekChar(1) == '*'.code) {
                multilineComment(buffer)
            } else {
                break
            }
        }

        return hasReadCharacters
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
        val start = sourceLines.getNextPosition()

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
            if (!nodeSpace(buffer)) {
                val c = peekChar()
                if (isNewline(c)) {
                    newline(buffer)
                } else if (c == '/'.code) {
                    val c2 = peekChar(1)
                    if (c2 != '/'.code) {
                        consumeChar()
                        throw KdlParseException(
                            "Unexpected character after '/'",
                            getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                            "'/' or '*' expected here"
                        )
                    }
                    singleLineComment(buffer)
                } else {
                    break
                }
            }
        }

        return Slashdash(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun quotedString(): StringToken {
        if (peekChar(1) == '"'.code) {
            if (peekChar(2) == '"'.code) {
                return multiLineQuotedString()
            }
        }
        return singleLineQuotedString()
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun multiLineQuotedString(): StringToken {
        val start = sourceLines.getNextPosition()
        consumeChar(3)
        expectNewLine()

        val lines = mutableListOf<MultilineStringLine>()

        while (true) {
            val prefix = readPrefix()
            val content = Buffer()
            if (readContent(start.line, content)) {
                lines.add(MultilineStringLine(prefix, content.readString()))
            } else {
                if (!content.exhausted()) {
                    val line: Int = sourceLines.getCurrentPosition().line
                    throw KdlParseException(
                        "Unexpected character in last line of multi-line string",
                        getErrorParseContext(start.line, line, Span.of(line, prefix.length + 1)),
                        "unexpected character",
                        "the last line of a multi-line string must only contain whitespaces"
                    )
                }
                val span = Span(start, sourceLines.getCurrentPosition())
                return StringToken(getMultilineStringValue(prefix, lines, span), span)
            }
        }
    }

    @Throws(IOException::class)
    private fun readPrefix(): String {
        val prefix = Buffer()

        while (true) {
            val c = peekChar()
            if (!isWhitespace(c)) {
                break
            }
            prefix.writeCodePointValue(readChar())
        }
        return prefix.readString()
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun readContent(startLine: Int, content: Buffer): Boolean {
        while (true) {
            val c = peekChar()
            if (c == EOF) {
                val nextPosition = sourceLines.getNextPosition()
                throw KdlParseException(
                    "Unexpected end of file in string",
                    getErrorParseContext(startLine, nextPosition.line, Span.of(nextPosition)),
                    "end of file"
                )
            } else if (c == '"'.code) {
                consumeChar()
                if (peekChar() == '"'.code) {
                    if (peekChar(1) == '"'.code) {
                        consumeChar(2)
                        return false
                    }
                }
                content.writeCodePointValue('"'.code)
            } else if (isNewline(c)) {
                consumeNewLine()
                return true
            } else {
                stringCharacter(content)
            }
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun getMultilineStringValue(
        lastLinePrefix: String,
        lines: List<MultilineStringLine>,
        span: Span
    ): String {
        val buffer = Buffer()

        for (i in lines.indices) {
            buffer.writeString(removeIndent(span, lastLinePrefix, span.start.line + i + 1, lines[i]))
            if (i < lines.size - 1) {
                buffer.writeCodePointValue('\n'.code)
            }
        }

        return buffer.readString()
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun removeIndent(span: Span, lastLinePrefix: String, lineNumber: Int, line: MultilineStringLine): String {
        if (line.content.isEmpty()) return ""

        if (!line.prefix.startsWith(lastLinePrefix)) {
            val errorSpan = Span(Position(lineNumber, 1), Position(lineNumber, max(1, line.prefix.length)))
            throw KdlParseException(
                "Invalid indentation in multi-line string",
                getErrorParseContext(span.start.line, span.end.line, errorSpan),
                "indentation does not match last line"
            )
        }

        return if (line.prefix.length > lastLinePrefix.length)
            line.prefix.substring(lastLinePrefix.length) + line.content else line.content
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun expectNewLine() {
        if (!isNewline(peekChar())) {
            throw KdlParseException(
                "Missing newline at start of multi-line string",
                getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                "newline expected"
            )
        }
        consumeNewLine()
    }

    @Throws(IOException::class)
    private fun consumeNewLine() {
        if (readChar() == CR && peekChar() == LF) consumeChar()
        sourceLines.newline()
    }

    private fun isWhitespaces(line: String): Boolean =
        line.toCharArray().all { isWhitespace(it.code) }

    private fun countSpaces(line: String): Int =
        line.toCharArray().takeWhile { isWhitespace(it.code) }.count()

    @Throws(IOException::class, KdlParseException::class)
    private fun singleLineQuotedString(): StringToken {
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
            } else if (isNewline(c)) {
                throw KdlParseException(
                    "Unexpected new line in string",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "new line",
                    "escape it or use a multi-line string"
                )
            } else {
                stringCharacter(buffer)
            }
        }

        return StringToken(buffer.readString(), Span(start, sourceLines.getCurrentPosition()))
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun stringCharacter(buffer: Buffer) {
        val c = readChar()
        if (c == '\\'.code) {
            escapedCharacter(buffer)
        } else {
            buffer.writeCodePointValue(c)
        }
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
        } else if (isWhitespace(c) || isNewline(c)) {
            whitespaceEscape()
        } else {
            throw KdlParseException(
                "Invalid escaped character '${c.toChar()}'",
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

    @Throws(IOException::class)
    private fun whitespaceEscape() {
        while (true) {
            val c = peekChar()
            if (!isWhitespace(c) && !isNewline(c)) return

            consumeChar()

            if (isNewline(c)) {
                if (c == CR && peekChar() == LF) consumeChar()
                sourceLines.newline()
            }
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun rawString(): StringToken {
        val start = sourceLines.getNextPosition()
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

        if (peekChar() == '"'.code) {
            if (peekChar(1) == '"'.code) {
                if (!isNewline(peekChar(2))) {
                    consumeChar(2)
                    throw KdlParseException(
                        "Newline required after opening quotes in multi-line raw string",
                        getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                        "new-line expected"
                    )
                }
                return multiLineRawString(start, openingSharpSigns)
            }
        }

        return singleLineRawString(start, openingSharpSigns)
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun multiLineRawString(start: Position, openingSharpSigns: Int): StringToken {
        consumeChar(2)
        expectNewLine()

        val lines = mutableListOf<String>()
        val currentLine = Buffer()

        while (true) {
            val c = readChar()
            if (c == EOF) {
                val nextPosition = sourceLines.getNextPosition()
                throw KdlParseException(
                    "Unexpected end of file in raw string",
                    getErrorParseContext(start.line, nextPosition.line, Span.of(nextPosition)),
                    "end of file"
                )
            } else if (isNewline(c)) {
                sourceLines.newline()
                lines.add(currentLine.readString())
            } else if (c == '"'.code && peekChar() == '"'.code && peekChar(1) == '"'.code) {
                consumeChar(2)
                var closingSharpSigns = 0
                while (peekChar() == '#'.code && closingSharpSigns < openingSharpSigns) {
                    consumeChar()
                    closingSharpSigns += 1
                }
                if (closingSharpSigns == openingSharpSigns) {
                    break
                }
                currentLine.writeString("\"\"\"")
                writeNonClosingSharpSigns(currentLine, closingSharpSigns)
            } else {
                currentLine.writeCodePointValue(c)
            }
        }

        val lastLine = currentLine.readString()
        val span = Span(start, sourceLines.getCurrentPosition())
        return StringToken(getMultiLineRawStringValue(lines, lastLine, span), span)
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun getMultiLineRawStringValue(lines: List<String>, lastLine: String, span: Span): String {
        checkLastLine(span, lastLine)
        val buffer = Buffer()

        for (i in lines.indices) {
            buffer.writeString(removeIndent(span, span.start.line + i + 1, lines[i], lastLine))
            if (i < lines.size - 1) {
                buffer.writeCodePointValue('\n'.code)
            }
        }

        return buffer.readString()
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun checkLastLine(span: Span, lastLine: String) {
        for ((column, line) in lastLine.withIndex()) {
            if (!isWhitespace(line.code)) {
                val errorSpan = Span.of(Position(span.end.line, column + 1))
                throw KdlParseException(
                    "Unexpected character in last line of multi-line string",
                    getErrorParseContext(span.start.line, span.end.line, errorSpan),
                    "unexpected character",
                    "the last line of a multi-line string must only contain whitespaces"
                )
            }
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun removeIndent(span: Span, lineNumber: Int, line: String, lastLine: String): String {
        if (isWhitespaces(line)) return ""

        if (!line.startsWith(lastLine)) {
            val errorSpan = Span(Position(lineNumber, 1), Position(lineNumber, max(countSpaces(line), 1)))
            throw KdlParseException(
                "Invalid indentation in multi-line string",
                getErrorParseContext(span.start.line, span.end.line, errorSpan),
                "indentation does not match last line"
            )
        }
        return line.substring(lastLine.length)
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun singleLineRawString(start: Position, openingSharpSigns: Int): StringToken {
        val buffer = Buffer()

        while (true) {
            val c = peekChar()
            if (c == EOF) {
                throw KdlParseException(
                    "Unexpected end of file in raw string",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "end of file"
                )
            } else if (isNewline(c)) {
                throw KdlParseException(
                    "Unexpected new line in raw string",
                    getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                    "new line"
                )
            } else if (c == '"'.code && peekChar(1) == '#'.code) {
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
            }
        }
    }

    @Throws(IOException::class, KdlParseException::class)
    private fun keyword(): Token {
        val start = sourceLines.getNextPosition()
        consumeChar()
        val buffer = Buffer()

        while (isIdentifierChar(peekChar())) {
            buffer.writeCodePointValue(readChar())
        }

        val span = Span(start, sourceLines.getCurrentPosition())
        return when (val keyword = buffer.readString()) {
            "true" -> BooleanToken(true, "#true", span)
            "false" -> BooleanToken(false, "#false", span)
            "null" -> Null(span)
            "inf" -> NumberToken.PositiveInfinity(span)
            "-inf" -> NumberToken.NegativeInfinity(span)
            "nan" -> NumberToken.NaN(span)
            else -> throw KdlParseException(
                "Invalid keyword '#$keyword'",
                getErrorParseContext(Span(start, start.withColumnOffset(keyword.length))),
                "unknown keyword"
            )
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
    private fun parseDigits(buffer: Buffer, digitPredicate: (Int) -> Boolean = KdlCharHelper::isDecimalDigit) {
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
    private fun invalidNumber(span: Span, number: String, base: IntegerBase): KdlParseException {
        return KdlParseException(
            "Invalid number ${base.prefix}$number",
            getErrorParseContext(span),
            "invalid number"
        )
    }

    @get:Throws(IOException::class)
    private val isIdentifierString: Boolean
        get() {
            val c = peekChar()
            if (isSign(c)) {
                val c2 = peekChar(1)
                if (c2 == '.'.code) {
                    val c3 = peekChar(2)
                    return !isDecimalDigit(c3)
                }
                return !isDecimalDigit(c2)
            }
            if (c == '.'.code) {
                val c2 = peekChar(1)
                return !isDecimalDigit(c2)
            }
            return isUnambiguousIdentifierChar(c)
        }

    @Throws(IOException::class, KdlParseException::class)
    private fun identifierString(): StringToken {
        val buffer = Buffer()
        val start = sourceLines.getNextPosition()

        while (isIdentifierChar(peekChar())) {
            buffer.writeCodePointValue(readChar())
        }

        val next = peekChar()
        if (next == '#'.code || next == '"'.code || next == '('.code || next == '['.code) {
            throw KdlParseException(
                "Invalid character '${next.toChar()}' in identifier",
                getErrorParseContext(Span.of(sourceLines.getNextPosition())),
                "unexpected character"
            )
        }

        val stringValue = buffer.readString()
        val span = Span(start, sourceLines.getCurrentPosition())

        if (isDisallowedIdentifier(stringValue)) {
            throw KdlParseException(
                "Keyword used as identifier",
                getErrorParseContext(span),
                "invalid identifier",
                "for the corresponding keyword use '#' (#$stringValue), for an identifier use quotes (\"$stringValue\")"
            )
        }

        return StringToken(stringValue, span)
    }

    private class MultilineStringLine(val prefix: String, val content: String)

    companion object {
        private fun writeNonClosingSharpSigns(buffer: Buffer, closingSharpSigns: Int) {
            if (closingSharpSigns > 0) {
                buffer.writeString("#".repeat(closingSharpSigns))
            }
        }

        private fun isInvalid(codepoint: Int): Boolean {
            return codepoint <= 8
                    || codepoint in 0x000E..0x01F
                    || codepoint == 0x007F
                    || codepoint in 0xD800..0xDFFF
                    || codepoint in 0x200E..0x200F
                    || codepoint in 0x202A..0x202E
                    || codepoint in 0x2066..0x2069
        }

        private const val READER_CAPACITY = 3

        private val ESCAPED_CHARACTERS: Map<Int, Int> = mapOf(
            '"'.code to '"'.code,
            '\\'.code to '\\'.code,
            'b'.code to '\b'.code,
            'f'.code to '\u000c'.code,
            'r'.code to '\r'.code,
            'n'.code to '\n'.code,
            't'.code to '\t'.code,
            's'.code to ' '.code
        )
    }
}
