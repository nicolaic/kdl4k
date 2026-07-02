package dev.kdl.parse

import dev.kdl.KdlDocument
import dev.kdl.parse.context.Position
import dev.kdl.parse.context.Span
import dev.kdl.parse.lexer.Lexer
import dev.kdl.parse.lexer.token.Token
import kotlinx.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.safeCast

internal abstract class KdlParserContext(protected val lexer: Lexer) {
    @Throws(IOException::class, KdlParseException::class)
    abstract fun parse(): KdlDocument

    @Throws(IOException::class, KdlParseException::class)
    protected open fun read(): Token? {
        return lexer.read()
    }

    @Throws(IOException::class, KdlParseException::class)
    protected fun peek(): Token? {
        return peek(0)
    }

    @Throws(IOException::class, KdlParseException::class)
    protected open fun peek(n: Int): Token? {
        return lexer.peek(n)
    }

    @Throws(IOException::class, KdlParseException::class)
    protected fun consume(n: Int): Unit = repeat(n) { read() }

    @Throws(IOException::class, KdlParseException::class)
    protected fun consumeToken(vararg tokenClasses: KClass<out Token>): Boolean {
        val token: Token? = peek()
        if (token != null) {
            for (tokenClass in tokenClasses) {
                if (tokenClass.isInstance(token)) {
                    read()
                    return true
                }
            }
        }
        return false
    }

    @Throws(IOException::class, KdlParseException::class)
    protected fun <TOKEN : Token> expectToken(
        tokenClass: KClass<TOKEN>,
        errorMessage: String,
        errorLabel: String,
        spanSupplier: () -> Span?
    ): TOKEN {
        val token: Token? = peek()
        if (tokenClass.isInstance(token)) {
            read()
            return tokenClass.cast(token)
        }
        var span: Span? = spanSupplier()
        if (span == null) {
            if (token == null) {
                span = Span.of(lexer.getNextPosition())
            } else {
                span = token.span
            }
        }
        throw KdlParseException(
            errorMessage,
            lexer.getErrorParseContext(span),
            errorLabel
        )
    }

    @Throws(IOException::class, KdlParseException::class)
    protected fun <TOKEN : Token> parseToken(tokenClass: KClass<TOKEN>): TOKEN? =
        peek().takeIf(tokenClass::isInstance)?.let { tokenClass.cast(read()) }

    @Throws(IOException::class, KdlParseException::class)
    protected fun expectClosingBrace(openingBracePosition: Position) {
        val token: Token? = peek()
        if (token is Token.Brace.ClosingBrace) {
            read()
            return
        }

        val span = token?.span ?: Span.of(lexer.getNextPosition())
        throw KdlParseException(
            "Missing closing brace at the end of children list",
            lexer.getErrorParseContext(openingBracePosition.line, span.end.line, span),
            "closing brace expected"
        )
    }
}
