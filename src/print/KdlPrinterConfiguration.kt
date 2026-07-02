package dev.kdl.print

import dev.kdl.KdlVersion
import kotlin.jvm.JvmRecord

/**
 * Configures a [KdlPrinter].
 *
 * @param version                  the version of KDL to use for printing
 * @param indentation              the whitespace characters used for a level of indentation
 * @param newline                  the newline characters used when printing a new lineNumber
 * @param exponentChar             the character used for the exponent of decimal numbers
 * @param printEmptyChildren       whether empty children should be printed
 * @param printNullArguments       whether null arguments should be printed
 * @param printNullProperties      whether null properties should be printed
 * @param printDuplicateProperties whether duplicate properties should be printed (only the last one is printed when false)
 * @param propertiesOrder          order to use when printing properties
 * @param printSemicolons          whether semicolons should be printed after each node
 * @param printQuotes              whether quotes should be printing around identifiers
 */
class KdlPrinterConfiguration(
    val version: KdlVersion = KdlVersion.V2,
    val indentation: List<Whitespace> = listOf(Whitespace.CHARACTER_TABULATION),
    val newline: List<Newline> = listOf(Newline.LF),
    val exponentChar: ExponentCharacter = ExponentCharacter.E,
    val printEmptyChildren: Boolean = false,
    val printNullArguments: Boolean = true,
    val printNullProperties: Boolean = true,
    val printDuplicateProperties: Boolean = true,
    val propertiesOrder: PropertiesOrder = PropertiesOrder.DECLARATION,
    val printSemicolons: Boolean = false,
    val printQuotes: Boolean = false,
) {
    val formattedIndentation by lazy { indentation.joinToString("") { it.value} }
    val formattedNewline by lazy { newline.joinToString("") { it.value} }
    /**
     * Valid newline characters that can be used when printing a KDL document.
     */
    enum class Newline(
        /**
         * @return string value of the newline character
         */
        val value: String
    ) {
        /**
         * Carriage return
         */
        CR("\r"),

        /**
         * Line feed
         */
        LF("\n"),

        /**
         * Carriage return + line feed
         */
        CRLF("\r\n"),

        /**
         * Next line
         */
        NEXT_LINE("\u0085"),

        /**
         * Form feed
         */
        FORM_FEED("\u000C"),

        /**
         * Line separator
         */
        LINE_SEPARATOR("\u2028"),

        /**
         * Paragraph separator
         */
        PARAGRAPH_SEPARATOR("\u2029");
    }

    /**
     * Valid characters to use for indentation.
     */
    enum class Whitespace(
        /**
         * @return the string corresponding to this whitespace
         */
        val value: String
    ) {
        /**
         * A horizontal tabulation (\t).
         */
        CHARACTER_TABULATION("\t"),

        /**
         * A line (vertical) tabulation.
         */
        LINE_TABULATION("\u000B"),

        /**
         * A regular space.
         */
        SPACE(" "),

        /**
         * A non-breaking space.
         */
        NO_BREAK_SPACE("\u00A0"),

        /**
         * Ogham space mark
         */
        OGHAM_SPACE_MARK("\u1680"),

        /**
         * En quad
         */
        EN_QUAD("\u2000"),

        /**
         * Em quad
         */
        EM_QUAD("\u2001"),

        /**
         * En space
         */
        EN_SPACE("\u2002"),

        /**
         * Em space
         */
        EM_SPACE("\u2003"),

        /**
         * Three-per-em space
         */
        THREE_PER_EM_SPACE("\u2004"),

        /**
         * Four-per-em space
         */
        FOUR_PER_EM_SPACE("\u2005"),

        /**
         * Six-per-em space
         */
        SIX_PER_EM_SPACE("\u2006"),

        /**
         * Figure Space
         */
        FIGURE_SPACE("\u2007"),

        /**
         * Punctuation space
         */
        PUNCTUATION_SPACE("\u2008"),

        /**
         * Thin space
         */
        THIN_SPACE("\u2009"),

        /**
         * Hair space
         */
        HAIR_SPACE("\u200A"),

        /**
         * Narrow non-breaking space
         */
        NARROW_NO_BREAK_SPACE("\u202F"),

        /**
         * Medium mathematical space
         */
        MEDIUM_MATHEMATICAL_SPACE("\u205F"),

        /**
         * Ideographic space
         */
        IDEOGRAPHIC_SPACE("\u3000");
    }

    /**
     * The characters that can be used for an exponential number.
     */
    enum class ExponentCharacter {
        /**
         * Lowercase e
         */
        e,

        /**
         * Uppercase E
         */
        E;

        /**
         * Replaces the exponential character in a string produced by [java.math.BigDecimal.toString] if required.
         *
         * @param decimalAsString a string representing a number
         * @return the same string with the exponential character replaced if required
         */
        fun replaceExponentCharacter(decimalAsString: String): String =
            if (this == E) decimalAsString else decimalAsString.replace('E', 'e')
    }

    /**
     * Different orders that can be used to sort properties.
     */
    enum class PropertiesOrder {
        /**
         * Order properties by declaration order.
         */
        DECLARATION,

        /**
         * Order properties by ascending name.
         */
        NAME_ASCENDING;

        /**
         * Sorts a collection of property names according to this order.
         *
         * @param propertyNames the names to sort
         * @return a sorted list of property names
         */
        fun sort(propertyNames: Collection<String>): List<String> = when (this) {
            DECLARATION -> propertyNames.toList()
            NAME_ASCENDING -> propertyNames.sorted()
        }
    }

    companion object
}
