package dev.kdl.parse.lexer.token

import dev.kdl.parse.context.Span

/**
 * A token produced by a KDL lexer.
 */
sealed class Token(
	/**
	 * The value of the token. The value is not always the same as the read characters.
	 *
	 * @return the value of the token
	 */
	val value: String,

	/**
	 * The position of the token in the input stream of file.
	 *
	 * @return a span for the token
	 */
	val span: Span,
) {
	override fun equals(other: Any?): Boolean =
		other is Token
			&& value == other.value
			&& span == other.span

	override fun hashCode(): Int {
		var result = value.hashCode()
		result = 31 * result + span.hashCode()
		return result
	}

	override fun toString(): String {
		return "${this::class.simpleName}(value='$value', span=$span)"
	}


	/**
	 * Token for a bare identifier (without quotes). It is only used by the KDL 1.0 lexer.
	 *
	 * @param value the value of the identifier
	 * @param span  the position of the token
	 */
	class BareIdentifier(value: String, span: Span) : Token(value, span)

	/**
	 * Token for a boolean.
	 *
	 * @param value the value of the token
	 * @param span  the position of the token
	 */
	class BooleanToken(val booleanValue: Boolean, value: String, span: Span) : Token(value, span)

	/**
	 * Token for braces.
	 */
	sealed class Brace(value: String, span: Span) : Token(value, span) {
		class OpeningBrace(span: Span) : Brace("{", span)
		class ClosingBrace(span: Span) : Brace("}", span)
	}

	/**
	 * Token for parentheses.
	 */
	sealed class Parentheses(value: String, span: Span) : Token(value, span) {
		class OpeningParentheses(span: Span) : Brace("(", span)
		class ClosingParentheses(span: Span) : Brace(")", span)
	}


	/**
	 * Token for the equals sign.
	 *
	 * @param span the position of the token
	 */
	class EqualsSign(span: Span) : Token("=", span)

	/**
	 * Token for a line continuation. Only used by the KDL 1.0 parser
	 *
	 * @param value the value of the line continuation, including newline characters.
	 * @param span  the position of the token
	 */
	class LineContinuation(value: String, span: Span) : Token(value, span)

	/**
	 * Token for a newline. Consecutive CR and LF characters (\r\n) are represented by one token.
	 *
	 * @param value the value of the newline
	 * @param span  the position of the token
	 */
	class Newline(value: String, span: Span) : Token(value, span)

	/**
	 * Token for node spaces. Only used by the KDL 2.0 parser.
	 *
	 * @param value the value of the node spaces
	 * @param span  the position of the token
	 */
	class NodeSpace(value: String, span: Span) : Token(value, span)

	/**
	 * Token for the null keyword.
	 *
	 * @param span the position of the token
	 */
	class Null(span: Span) : Token("#null", span)


	/**
	 * Token for a semicolon.
	 *
	 * @param span the span of the token
	 */
	class Semicolon(span: Span) : Token(";", span)

	/**
	 * Token for a single-line comment.
	 *
	 * @param value the value of the comment
	 * @param span  the position of the token
	 */
	class SingleLineComment(value: String, span: Span) : Token(value, span)

	/**
	 * Token for a slashdash, including all the spaces after.
	 *
	 * @param value the value of the token
	 * @param span  the position of the token
	 */
	class Slashdash(value: String, span: Span) : Token(value, span)

	/**
	 * A token for a string value. It matches identifiers, quoted strings, raw strings, multi-line strings, and multi-line
	 * raw strings.
	 *
	 * @param value the content of the string, with newlines appropriately escaped
	 * @param span  the position of the token
	 */
	class StringToken(value: String, span: Span) : Token(value, span)

	/**
	 * Token for whitespaces.
	 *
	 * @param value the value of the token
	 * @param span  the position of the token
	 */
	class Whitespace(value: String, span: Span) : Token(value, span)
}
