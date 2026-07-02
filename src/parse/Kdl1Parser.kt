package dev.kdl.parse

import dev.kdl.*
import dev.kdl.parse.context.Span
import dev.kdl.parse.lexer.Kdl1Lexer
import dev.kdl.parse.lexer.token.NumberToken
import dev.kdl.parse.lexer.token.Token
import dev.kdl.parse.lexer.token.Token.*
import kotlinx.io.IOException
import kotlinx.io.Source

/**
 * Parser for the KDL 1.0 syntax.
 */
class Kdl1Parser : KdlParser {
    @Throws(IOException::class, KdlParseException::class)
    override fun parse(source: Source, filename: String?): KdlDocument {
        return Kdl1ParserContext(filename, source).parse()
    }

    private class Kdl1ParserContext(filename: String?, source: Source) :
        KdlParserContext(Kdl1Lexer(filename, source, LEXER_CAPACITY)) {
        @Throws(IOException::class, KdlParseException::class)
        override fun parse(): KdlDocument {
            return KdlDocument(nodes())
        }

        @Throws(IOException::class, KdlParseException::class)
        fun nodes(): List<KdlNode> {
            val nodes = mutableListOf<KdlNode>()

            while (true) {
                lineSpaces()
                val slashdash = parseToken(Slashdash::class)
                val node: KdlNode? = node()

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
            }

            lineSpaces()

            val token = lexer.peek()
            if (token is LineContinuation) {
                throw KdlParseException(
                    "unexpected line continuation between nodes",
                    lexer.getErrorParseContext(token.span),
                    "line continuation",
                    "line continuations can only be used inside a node"
                )
            }

            return nodes
        }

        @Throws(IOException::class, KdlParseException::class)
        fun node(): KdlNode? {
            val type = type()

            val name = identifier() ?: if (type == null) {
                return null
            } else {
                throw KdlParseException(
                    "Missing node name after node type",
                    lexer.getErrorParseContextForNextPosition(),
                    "string expected"
                )
            }

            val argumentsAndProperties = argumentsAndProperties()
            val children = children()
            nodeSpaces()
            nodeTerminator()

            return KdlNode(
                name,
                type,
                argumentsAndProperties.first,
                argumentsAndProperties.second,
                children
            )
        }

        @Throws(IOException::class, KdlParseException::class)
        fun argumentsAndProperties(): Pair<List<KdlValue<*>>, KdlProperties> {
            val arguments = mutableListOf<KdlValue<*>>()
            val properties = KdlProperties.builder()

            while (true) {
                if (nodeSpaces() && this.isArgumentOrProperty) {
                    val isSlashdash = consumeToken(Slashdash::class)
                    val propertyName: Token? = this.propertyName
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
                } else {
                    break
                }
            }

            return Pair(arguments, properties.build())
        }

        @get:Throws(IOException::class, KdlParseException::class)
        val isArgumentOrProperty: Boolean
            get() {
                if (peek(0) is Slashdash) {
                    return isValueStart(peek(1))
                }
                return isValueStart(peek(0))
            }

        fun isValueStart(token: Token?): Boolean {
            return token is Parentheses.OpeningParentheses
                    || token is NumberToken
                    || token is BooleanToken
                    || token is Null
                    || token is StringToken
                    || token is BareIdentifier
        }

        @get:Throws(IOException::class, KdlParseException::class)
        val propertyName: Token?
            get() {
                val name = peek(0)
                if ((name is StringToken || name is BareIdentifier) && peek(1) is EqualsSign) {
                    consume(2)
                    return name
                }
                return null
            }

        @Throws(IOException::class, KdlParseException::class)
        fun expectValue(errorMessage: String?): KdlValue<*> {
            val type = type()

            val token = read()
            if (token is NumberToken) {
                return token.asKDLNumber(type)
            } else if (token is BooleanToken) {
                return KdlBoolean(token.booleanValue, type)
            } else if (token is Null) {
                return KdlNull(type)
            } else if (token is StringToken) {
                return KdlString(token.value, type)
            }

            val errorSpan = token?.span ?: Span.of(lexer.getNextPosition())

            throw KdlParseException(
                errorMessage!!,
                lexer.getErrorParseContext(errorSpan),
                "value expected"
            )
        }

        @Throws(IOException::class, KdlParseException::class)
        fun children(): List<KdlNode> {
            nodeSpaces()

            val openingBrace = this.openingBrace
            if (openingBrace != null) {
                val nodes = nodes()
                expectClosingBrace(openingBrace.second.span.start)
                if (openingBrace.first == null) {
                    return nodes
                }
            }

            return emptyList()
        }

        @get:Throws(IOException::class, KdlParseException::class)
        val openingBrace: Pair<Slashdash?, Brace.OpeningBrace>?
            get() {
                val token = peek(0)
                if (token is Slashdash) {
                    val secondToken = peek(1)
                    if (secondToken is Brace.OpeningBrace) {
                        consume(2)
                        return Pair(token, secondToken)
                    }
                } else if (token is Brace.OpeningBrace) {
                    consume(1)
                    return null to token
                }
                return null
            }

        @Throws(IOException::class, KdlParseException::class)
        fun type(): String? {
            val openingParentheses = parseToken(Parentheses.OpeningParentheses::class) ?: return null
            val type = read() ?: return null

            if ((type is BareIdentifier || type is StringToken).not()) {
                throw KdlParseException(
                    "Missing type name in type annotation",
                    lexer.getErrorParseContext(type.span),
                    "string or identifier expected"
                )
            }

            expectToken(
                Parentheses.ClosingParentheses::class,
                "Missing closing parentheses in type annotation",
                "closing parentheses expected",
                { Span(openingParentheses.span.start, type.span.end) }
            )

            return type.value
        }

        @Throws(IOException::class, KdlParseException::class)
        fun identifier(): String? {
            val token = peek()

            if (token is StringToken || token is BareIdentifier) {
                consume(1)
                return token.value
            }

            return null
        }

        @Throws(IOException::class, KdlParseException::class)
        fun nodeTerminator() {
            val token = lexer.read()
            if (!(token == null || token is SingleLineComment || token is Newline || token is Semicolon)) {
                throw KdlParseException(
                    "missing node terminator",
                    lexer.getErrorParseContext(token.span),
                    "unexpected character",
                    "a node must be terminated by a newline, a single-line comment, a semicolon or the end of file"
                )
            }
        }

        @Throws(IOException::class, KdlParseException::class)
        fun lineSpaces() {
            while (true) {
                if (!consumeToken(Whitespace::class, Newline::class, SingleLineComment::class)) {
                    break
                }
            }
        }

        @Throws(IOException::class, KdlParseException::class)
        fun nodeSpaces(): Boolean {
            var hasLength = false

            while (consumeToken(Whitespace::class, LineContinuation::class)) {
                hasLength = true
            }

            return hasLength
        }

        companion object {
            private const val LEXER_CAPACITY = 2
        }
    }
}
