package dev.kdl.parse.lexer.token

import dev.kdl.parse.context.Span

/**
 * Token for the byte-order mark character (0xFEFF).
 *
 * @param span the position of the token
 */
class ByteOrderMark(span: Span) : Token(STRING_VALUE, span) {

    companion object {
        const val VALUE: Char = 0xFEFF.toChar()
        private const val STRING_VALUE = VALUE.toString()
    }
}
