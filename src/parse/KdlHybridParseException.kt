package dev.kdl.parse


/**
 * @return the exception thrown by the KDL 1.0 parser
 */
/**
 * @return the exception thrown by the KDL 2.0 parser
 */
/**
 * An exception thrown by the [KdlHybridParser] when both the KDL 2.0 and the KDL 1.0 parsers fail to parse the
 * document. Contains both exceptions.
 */
class KdlHybridParseException
/**
 * Creates a new [KdlHybridParseException]
 * 
 * @param v1Exception the parse exception thrown by the KDL 1.0 parser
 * @param v2Exception the parse exception thrown by the KDL 2.0 parser
 */(
    /**
     * The exception thrown by the KDL 1.0 parser
     */
    val v1Exception: KdlParseException,
    /**
     * The exception thrown by the KDL 2.0 parser
     */
    val v2Exception: KdlParseException
) : KdlParseException("Failed to parse the document using both the KDL v2 and the KDL v1 parser.")
