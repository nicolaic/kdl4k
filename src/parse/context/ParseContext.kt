package dev.kdl.parse.context

import kotlin.jvm.JvmRecord

/**
 * A context for a parse.
 * 
 * @param filename    the name of the file being read
 * @param sourceLines a list of [SourceLine]
 * @param span        a span for the part of the source that is described by this context
 */
@JvmRecord
data class ParseContext(
    val filename: String?,
    val sourceLines: List<SourceLine>,
    val span: Span,
)
