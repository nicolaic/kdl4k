package dev.kdl.parse.lexer

import dev.kdl.parse.KdlInternalParseException
import dev.kdl.parse.KdlParseException
import dev.kdl.parse.context.ParseContext
import dev.kdl.parse.context.Position
import dev.kdl.parse.context.SourceLines
import dev.kdl.parse.context.Span
import dev.kdl.parse.lexer.helper.Kdl2CharHelper.isNewline
import dev.kdl.parse.lexer.reader.KdlReader
import dev.kdl.parse.lexer.reader.KdlReader.Companion.EOF
import dev.kdl.parse.lexer.token.Token
import kotlinx.io.IOException

abstract class AbstractKdlLexer(
	private val filename: String?,
	private val reader: KdlReader,
	capacity: Int,
) : Lexer {
	private val readTokens: RingBuffer<Token> = RingBuffer(capacity)

	/**
	 * The source lines for the parsed document.
	 */
	protected val sourceLines: SourceLines = SourceLines()

	/**
	 * Reads the stream for the next token.
	 *
	 * @return the next token, or `null` if the end of file has been reached.
	 * @throws IOException       when an error occurs while reading
	 * @throws KdlParseException when there is a syntax error
	 */
	@Throws(IOException::class, KdlParseException::class)
	protected abstract fun nextToken(): Token?

	@Throws(IOException::class, KdlParseException::class)
	override fun read(): Token? {
		if (!readTokens.isEmpty) {
			return readTokens.removeFirst()
		}
		return nextToken()
	}

	@Throws(IOException::class, KdlParseException::class)
	override fun peek(n: Int): Token? {
		if (n < 0 || n >= readTokens.capacity()) {
			throw KdlInternalParseException("Error while peeking: n should be between 0 and " + (readTokens.capacity() - 1) + " included but was " + n)
		}

		while (readTokens.size() <= n) {
			readTokens.addLast(nextToken() ?: return null)
		}

		return readTokens.get(n)
	}

	@Throws(IOException::class)
	override fun getErrorParseContext(startLine: Int, endLine: Int, span: Span): ParseContext {
		while (true) {
			val c = readChar()
			if (c == EOF || isNewline(c)) {
				sourceLines.newline()
				break
			}
		}
		return ParseContext(filename, sourceLines.getLines(startLine, endLine), span)
	}

	@Throws(IOException::class)
	override fun getErrorParseContextForNextPosition(): ParseContext = if (readTokens.isEmpty) {
		getErrorParseContext(Span.of(sourceLines.getNextPosition()))
	} else {
		getErrorParseContext(readTokens.get(0)!!.span)
	}

	override fun getNextPosition(): Position = if (readTokens.isEmpty) {
		sourceLines.getNextPosition()
	} else {
		readTokens.get(0)!!.span.start
	}

	/**
	 * Reads the next character.
	 *
	 * @return the next character, or [KdlReader.EOF] if the end of file has been reached
	 * @throws IOException when there is an error reading the stream
	 */
	@Throws(IOException::class)
	protected fun readChar(): Int {
		val c = reader.read()
		if (c != EOF) sourceLines.append(c)
		return c
	}

	/**
	 * Consumes the next characters.
	 *
	 * @param n the number of characters to consume
	 * @throws IOException when there is an error reading the stream
	 */
	@Throws(IOException::class)
	protected fun consumeChar(n: Int = 1): Unit = repeat(n) { readChar() }

	/**
	 * Peeks a character ahead.
	 *
	 * @param n the index of the character to peek, 0-based.
	 * @return the peeked character, or [KdlReader.EOF] if the end of file has been reached
	 * @throws IOException when there is an error reading the stream
	 */
	@Throws(IOException::class)
	protected fun peekChar(n: Int = 0): Int = reader.peek(n)

	/**
	 * Consumes the next character and creates a new token with the provided function.
	 *
	 * @param createToken a function that creates a token from its span
	 * @param <T>         the type of the created token
	 * @return a new token created with `createToken`
	 * @throws IOException when there is an error reading the stream
	</T> */
	@Throws(IOException::class)
	protected fun <T : Token> consumeAndCreate(createToken: (Span) -> T): T {
		consumeChar()
		return createToken(Span.of(sourceLines.getCurrentPosition()))
	}

}
