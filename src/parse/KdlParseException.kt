package dev.kdl.parse

import dev.kdl.parse.context.ParseContext

/**
 * An exception thrown when a KDL document is invalid. The error can be printed for users using a [Reporter].
 */
open class KdlParseException(
    /**
     * The main error message.
     */
    val errorMessage: String,
    /**
     * A context of the error
     */
    val context: ParseContext? = null,
    /**
     * A label for the highlighted section of the context
     */
    val label: String? = null,
    /**
     * A help message for the user
     */
    val help: String? = null
) : Exception(computeMessage(errorMessage, context)) {

    companion object {
        /**
         * Creates a new [KdlParseException].
         *
         * @param errorMessage the main error message
         * @param context      a context of the error
         */
        private fun computeMessage(errorMessage: String, context: ParseContext?): String {
            return if (context == null) errorMessage
            else "$errorMessage at ${context.span.start.line}:${context.span.start.column}"
        }
    }
}
