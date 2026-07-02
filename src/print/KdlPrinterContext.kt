package dev.kdl.print

import dev.kdl.*
import dev.kdl.KdlNumber.NotANumber
import kotlinx.io.*

/**
 * Creates a new printing context.
 *
 * @param sink          the sink to write to
 * @param configuration the configuration to use when printing the document
 */
abstract class KdlPrinterContext protected constructor(
    private val sink: Sink,
    private val configuration: KdlPrinterConfiguration,
) {
    private var depth = 0

    /**
     * Checks if a codepoint is an identifier character.
     *
     * @param c a codepoint
     * @return true if c is an identifier character, false otherwise
     */
    protected abstract fun isIdentifierChar(c: Int): Boolean

    /**
     * Prints a null value.
     *
     * @param kdlNull a null value to print
     */
    protected abstract fun printNull(kdlNull: KdlNull)

    /**
     * Prints a boolean.
     *
     * @param kdlBoolean a boolean to print
     */
    protected abstract fun printBoolean(kdlBoolean: KdlBoolean)

    /**
     * Prints a NaN value.
     *
     * @param notANumber a NaN value to print
     */
    protected abstract fun printNotANumber(notANumber: NotANumber)

    /**
     * Prints a positive infinity number.
     *
     * @param positiveInfinity a positive infinity number to print
     */
    protected abstract fun printPositiveInfinity(positiveInfinity: KdlNumber.PositiveInfinity)

    /**
     * Prints a negative infinity number.
     *
     * @param negativeInfinity a negative infinity number to print
     */
    protected abstract fun printNegativeInfinity(negativeInfinity: KdlNumber.NegativeInfinity)

    /**
     * Tries to escape a codepoint for use in a string.
     *
     * @param c the codepoint to escape
     * @return a string containing the escaped codepoint, or `null` if no escaping is required
     */
    protected open fun escape(c: Int): String? = when (c) {
        '\n'.code -> "\\n"
        '\r'.code -> "\\r"
        '\t'.code -> "\\t"
        '\\'.code -> "\\\\"
        '"'.code -> "\\\""
        '\b'.code -> "\\b"
        '\u000c'.code -> "\\f"
        else -> null
    }

    /**
     * Checks if a codepoint can be a valid first character for an identifier.
     *
     * @param c a codepoint
     * @return `true` if the codepoint can start an identifier, `false` otherwise
     */
    protected abstract fun isValidStartOfIdentifier(c: Int): Boolean

    fun printDocument(document: KdlDocument) = try {
		printNodes(document.nodes)
	} catch (e: KdlPrintException) {
		throw e.cause
	}

    private fun printNode(node: KdlNode) {
        printType(node.type)
        writeIdentifier(node.name)

        for (argument in node.arguments) {
            if (configuration.printNullArguments || argument !is KdlNull) {
                write(' ')
                printValue(argument)
            }
        }

        val properties = configuration.propertiesOrder.sort(node.properties.propertyNames())
        for (property in properties) {
            val values = node.properties.getValues(property)

            if (configuration.printDuplicateProperties) {
                values.forEach({ value -> printProperty(property, value) })
            } else if (!values.isEmpty()) {
                printProperty(property, values.last())
            }
        }

        if (configuration.printEmptyChildren || !node.children.isEmpty()) {
            write(" {")
            write(configuration.formattedNewline)
            depth += 1
            printNodes(node.children)
            depth -= 1
            printIndentation()
            write("}")
        }
    }

    private fun printValue(value: KdlValue<*>) = when (value) {
        is KdlNull -> printNull(value)
        is KdlString -> printString(value)
        is KdlBoolean -> printBoolean(value)
        is KdlNumber<*> -> printNumber(value)
    }

    private fun printProperty(name: String, value: KdlValue<*>) {
        if (configuration.printNullProperties || value !is KdlNull) {
            write(' ')
            writeIdentifier(name)
            write('=')
            printValue(value)
        }
    }

    /**
     * Prints a string.
     *
     * @param string the string to print
     */
    protected open fun printString(string: KdlString) {
        printType(string.type)
        writeIdentifier(string.value)
    }

    private fun printNodes(nodes: List<KdlNode>) {
        if (nodes.isEmpty() && depth == 0) {
            write(configuration.formattedNewline)
            return
        }

        for (node in nodes) {
            printIndentation()
            printNode(node)
            if (configuration.printSemicolons) {
                write(';')
            }
            write(configuration.formattedNewline)
        }
    }

    private fun printNumber(number: KdlNumber<*>) = when (number) {
        is NotANumber -> printNotANumber(number)
        is KdlNumber.PositiveInfinity -> printPositiveInfinity(number)
        is KdlNumber.NegativeInfinity -> printNegativeInfinity(number)
        is KdlNumber.Integer -> printInteger(number)
        is KdlNumber.Decimal -> printDecimal(number)
    }

    private fun printInteger(integer: KdlNumber.Integer) {
        printType(integer.type)
        write(integer.value.toString())
    }

    private fun printDecimal(decimal: KdlNumber.Decimal) {
        printType(decimal.type)
        write(configuration.exponentChar.replaceExponentCharacter(decimal.value.toString()))
    }

    private fun printIndentation() {
        repeat(depth) { write(configuration.formattedIndentation) }
    }

    /**
     * Prints the type of node or value.
     *
     * @param type the type to print
     */
    protected fun printType(type: String?) {
        if (type != null) {
            write('(')
            writeIdentifier(type)
            write(')')
        }
    }

    private fun writeIdentifier(string: String) {
        if (string.isEmpty()) {
            write("\"\"")
        } else {
			writeEscapedString(string)
        }
    }

    /**
     * Escapes and writes character that need to be escaped in a single-line quoted string.
     *
     * @param string  the content of the string to escape and write
     */
    protected fun writeEscapedString(
		string: String,
		printQuotes: Boolean = configuration.printQuotes,
	) {
		val inputBuffer = Buffer().also { it.writeString(string) }
		val outputBuffer = Buffer()

        var needsQuotes = !isValidStartOfIdentifier(inputBuffer.peek().readCodePointValue())

		while (inputBuffer.exhausted().not()) {
			val c = inputBuffer.readCodePointValue()
			// TODO: Replace escape logic with more optimized solution
            val escaped = escape(c)
            if (escaped != null) {
                needsQuotes = true
				outputBuffer.writeString(escaped)
            } else {
                needsQuotes = needsQuotes or !isIdentifierChar(c)
                outputBuffer.writeCodePointValue(c)
            }
        }

		// TODO: Replace write(outputBuffer.readString())
		if (printQuotes || needsQuotes) {
			write('"')
			write(outputBuffer.readString())
			write('"')
		} else {
			write(outputBuffer.readString())
		}
    }

    /**
     * Writes a character
     *
     * @param c the character to write
     */
    protected fun write(c: Char) = try {
        sink.writeCodePointValue(c.code)
    } catch (e: IOException) {
        throw KdlPrintException(e)
    }

    /**
     * Writes a string
     *
     * @param string the string to write
     */
    protected fun write(string: String) = try {
        sink.writeString(string)
    } catch (e: IOException) {
        throw KdlPrintException(e)
    }
}
