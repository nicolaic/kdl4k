package dev.kdl.parse

import dev.kdl.*
import dev.kdl.parse.context.Span
import dev.kdl.parse.lexer.Kdl2Lexer
import dev.kdl.parse.lexer.token.ByteOrderMark
import dev.kdl.parse.lexer.token.NumberToken
import dev.kdl.parse.lexer.token.Token
import dev.kdl.parse.lexer.token.Token.*
import dev.kdl.parse.lexer.token.Token.Brace.OpeningBrace
import dev.kdl.parse.lexer.token.Token.Parentheses.OpeningParentheses
import kotlinx.io.IOException
import kotlinx.io.Source

/**
 * Parser for the KDL 2.0 syntax.
 */
class Kdl2Parser : KdlParser {
    @Throws(IOException::class, KdlParseException::class)
    override fun parse(source: Source, filename: String?): KdlDocument {
        return Kdl2ParserContext(filename, source).parse()
    }

    private class Kdl2ParserContext(filename: String?, source: Source) :
        KdlParserContext(Kdl2Lexer(filename, source, LEXER_CAPACITY)) {
        @Throws(IOException::class, KdlParseException::class)

        override fun parse(): KdlDocument {
            return document()
        }

        @Throws(IOException::class, KdlParseException::class)
        fun document(): KdlDocument {
            consumeByteOrderMark()
            return KdlDocument(nodes(true))
        }

        @Throws(IOException::class, KdlParseException::class)
        fun consumeByteOrderMark() {
            if (lexer.peek() is ByteOrderMark) {
                lexer.read()
            }
        }

        @Throws(IOException::class, KdlParseException::class)
        fun nodes(isRoot: Boolean): List<KdlNode> {
            val nodes = mutableListOf<KdlNode>()

            while (true) {
                lineSpaces()
                val slashdash = parseToken(Slashdash::class)
                val node = node()

                if (node == null) {
                    if (slashdash != null) {
                        val errorSpan = Span.of(lexer.getNextPosition())
                        throw KdlParseException(
                            "Valid node expected after slashdash",
                            lexer.getErrorParseContext(
                                slashdash.span.start.line,
                                errorSpan.end.line,
                                errorSpan
                            ),
                            "node expected"
                        )
                    }
                    break
                }

                if (slashdash == null) {
                    nodes.add(node)
                }

                if (!nodeTerminator() && (isRoot || peek() !is Brace.ClosingBrace)) {
                    throw KdlParseException(
                        "Semi-colon expected between nodes on the same line",
                        lexer.getErrorParseContext(Span.of(lexer.getNextPosition())),
                        "semi-colon expected",
                        "nodes need to be separated by a semi-colon or a newline character"
                    )
                }
            }

            lineSpaces()

            return nodes
        }

        @Throws(KdlParseException::class, IOException::class)
        fun node(): KdlNode? {
            val type = type()
            consumeToken(NodeSpace::class)

            val name = string() ?: if (type == null) {
                return null
            } else {
                throw KdlParseException(
                    "Missing node name after node type",
                    lexer.getErrorParseContextForNextPosition(),
                    "string expected"
                )
            }

            val argumentsAndProperties: Pair<List<KdlValue<*>>, KdlProperties> = argumentsAndProperties()
            val children: List<KdlNode> = nodeChildren()
            consumeToken(NodeSpace::class)

            return KdlNode(
                name,
                type,
                argumentsAndProperties.first,
                argumentsAndProperties.second,
                children
            )
        }

        @Throws(IOException::class, KdlParseException::class)
        fun type(): String? {
            val openingParentheses = parseToken(OpeningParentheses::class) ?: return null

            val nodeSpace = parseToken(NodeSpace::class)
            val type = expectToken(
                StringToken::class,
                "Missing type name in type annotation",
                "string expected",
                { nodeSpace?.span }
            )
            consumeToken(NodeSpace::class)
            expectToken(
                Parentheses.ClosingParentheses::class,
                "Missing closing parentheses in type annotation",
                "closing parentheses expected",
                { Span(openingParentheses.span.start, type.span.end) }
            )

            return type.value
        }

        @Throws(IOException::class, KdlParseException::class)
        fun argumentsAndProperties(): Pair<List<KdlValue<*>>, KdlProperties> {
            val arguments = mutableListOf<KdlValue<*>>()
            val properties = KdlProperties.builder()

            while (this.isArgumentOrProperty) {
                consumeToken(NodeSpace::class)
                val isSlashdash = consumeToken(Slashdash::class)

                val propertyName = this.propertyName
                if (propertyName != null) {
                    val value: KdlValue<*> = expectValue("Missing property value")
                    if (!isSlashdash) {
                        properties.property(propertyName.value, value)
                    }
                } else {
                    val value: KdlValue<*> = expectValue("Missing value after argument type")
                    if (!isSlashdash) {
                        arguments.add(value)
                    }
                }
            }

            return Pair(arguments, properties.build())
        }

        @get:Throws(IOException::class, KdlParseException::class)
        val isArgumentOrProperty: Boolean
            get() {
                if (peek(0) is NodeSpace) {
                    if (peek(1) is Slashdash) {
                        return isValueStart(peek(2))
                    } else {
                        return isValueStart(peek(1))
                    }
                }
                return peek(0) is Slashdash && isValueStart(peek(1))
            }

        fun isValueStart(token: Token?): Boolean {
            return token is OpeningParentheses
                    || token is NumberToken
                    || token is BooleanToken
                    || token is Null
                    || token is StringToken
        }

        @get:Throws(IOException::class, KdlParseException::class)
        val propertyName: StringToken?
            get() {
                val firstToken = peek(0)
                if (firstToken is StringToken) {
                    if (peek(1) is NodeSpace && peek(2) is EqualsSign) {
                        consume(3)
                        return firstToken as StringToken?
                    } else if (peek(1) is EqualsSign) {
                        consume(2)
                        return firstToken as StringToken?
                    }
                }
                return null
            }

        @Throws(IOException::class, KdlParseException::class)
        fun expectValue(errorMessage: String?): KdlValue<*> {
            val type = type()
            consumeToken(NodeSpace::class)

            return when (val token = read()) {
                is NumberToken -> token.asKDLNumber(type)
                is BooleanToken -> KdlBoolean(token.booleanValue, type)
                is Null -> KdlNull(type)
                is StringToken -> KdlString(token.value, type)
                else -> {
                    val errorSpan = token?.span ?: Span.of(lexer.getNextPosition())
                    throw KdlParseException(
                        errorMessage!!,
                        lexer.getErrorParseContext(errorSpan),
                        "value expected"
                    )
                }
            }
        }

        @Throws(IOException::class, KdlParseException::class)
        fun nodeChildren(): List<KdlNode> {
            var children: List<KdlNode>? = null

            var openingBrace: Pair<Slashdash?, OpeningBrace>? = this.openingBrace
            while (openingBrace != null) {
                if (openingBrace.first != null) {
                    nodes(false)
                    expectClosingBrace(openingBrace.second.span.start)
                } else if (children != null) {
                    throw KdlParseException(
                        "More than one list of children provided for node",
                        lexer.getErrorParseContext(openingBrace.second.span),
                        "second children list"
                    )
                } else {
                    children = nodes(false)
                    expectClosingBrace(openingBrace.second.span.start)
                }
                openingBrace = this.openingBrace
            }

            return children ?: emptyList()
        }

        @get:Throws(IOException::class, KdlParseException::class)
        val openingBrace: Pair<Slashdash?, OpeningBrace>?
            get() {
                val offset = if (peek(0) is NodeSpace) 1 else 0
                val token = peek(offset)
                if (token is Slashdash) {
                    val nextToken = peek(offset + 1)
                    if (nextToken is OpeningBrace) {
                        consume(offset + 2)
                        return token to nextToken
                    }
                } else if (token is OpeningBrace) {
                    consume(offset + 1)
                    return null to token
                }
                return null
            }

        @Throws(IOException::class, KdlParseException::class)
        fun nodeTerminator(): Boolean {
            return peek() == null || consumeToken(
                SingleLineComment::class,
                Newline::class,
                Semicolon::class
            )
        }

        @Throws(IOException::class, KdlParseException::class)
        fun string(): String? = parseToken(StringToken::class)?.value

        @Throws(IOException::class, KdlParseException::class)
        fun lineSpaces() {
            while (true) {
                if (!consumeToken(NodeSpace::class, Newline::class, SingleLineComment::class)) {
                    break
                }
            }
        }

        @Throws(IOException::class, KdlParseException::class)
        override fun read(): Token? {
            return checkBom(lexer.read())
        }

        @Throws(IOException::class, KdlParseException::class)
        override fun peek(n: Int): Token? {
            return checkBom(lexer.peek(n))
        }

        @Throws(KdlParseException::class, IOException::class)
        fun checkBom(token: Token?): Token? {
            if (token is ByteOrderMark) {
                throw KdlParseException(
                    "Unexpected byte-order mark after start",
                    lexer.getErrorParseContext(token.span),
                    "byte-order mark",
                    "byte-order mark can only appear as the first character of the document"
                )
            }

            return token
        }

        companion object {
            private const val LEXER_CAPACITY = 3
        }
    }
}
