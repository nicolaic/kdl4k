package dev.kdl.parse

import dev.kdl.parse.context.SourceLine
import dev.kdl.parse.context.Span
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

internal class ReporterTest {

    @ParameterizedTest
    @MethodSource("testCases")
    fun reportTest(exception: KdlParseException, expected: String) {
        val result = Reporter.getReport(exception)
        assertEquals(expected, result)
    }

    companion object {
        @JvmStatic
        fun testCases(): List<Arguments> = listOf(
            Arguments.of(KdlParseException("Something went wrong"), "× Something went wrong"),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    help = "you should consider fixing this"
                ),
                "× Something went wrong\nhelp: you should consider fixing this"
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        null, listOf(SourceLine(1, "first and only line")),
                        Span.of(1, 7, 1, 9)
                    )
                ),
                """
                × Something went wrong:
                  ╭─[1:7]
                1 │ first and only line
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        "test.kdl",
                        listOf(SourceLine(1, "first and only line")),
                        Span.of(1, 7, 1, 9)
                    )
                ),
                """
                × Something went wrong:
                  ╭─[test.kdl:1:7]
                1 │ first and only line
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        "test.kdl",
                        listOf(SourceLine(1, "first and only line")),
                        Span.of(1, 7, 1, 9)
                    ),
                    "this is a very long label"
                ),
                """
                × Something went wrong:
                  ╭─[test.kdl:1:7]
                1 │ first and only line
                  ·       ─┬─
                  ·        ╰ this is a very long label
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        "test.kdl",
                        listOf(SourceLine(1, "first and only line")),
                        Span.of(1, 7, 1, 9)
                    ),
                    "short"
                ),
                """
                × Something went wrong:
                  ╭─[test.kdl:1:7]
                1 │ first and only line
                  ·       ─┬─
                  ·  short ╯
                  ╰─
                  """.trimIndent()
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        "test.kdl",
                        listOf(SourceLine(1, "first and only line")),
                        Span.of(1, 7, 1, 9)
                    ),
                    "short",
                    "you should consider fixing this"
                ),
                """
                × Something went wrong:
                  ╭─[test.kdl:1:7]
                1 │ first and only line
                  ·       ─┬─
                  ·  short ╯
                  ╰─
                help: you should consider fixing this
                """.trimIndent()
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        "test.kdl",
                        listOf(
                            SourceLine(33, "line thirty-three"),
                            SourceLine(34, "line thirty-four"),
                            SourceLine(35, "last line (thirty-five)")
                        ),
                        Span.of(33, 6, 35, 9)
                    ),
                    "this is a very long label"
                ),
                """
                × Something went wrong:
                   ╭─[test.kdl:33:6]
                33 │ line thirty-three
                   ·      ────────────
                34 │ line thirty-four
                   · ────────────────
                35 │ last line (thirty-five)
                   · ────┬────
                   ·     ╰ this is a very long label
                   ╰─
                   """.trimIndent()
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        "test.kdl",
                        listOf(
                            SourceLine(33, "line thirty-three"),
                            SourceLine(34, "line thirty-four"),
                            SourceLine(35, "last line (thirty-five)")
                        ),
                        Span.of(33, 13, 35, 9)
                    ),
                    "this is a very long label"
                ),
                """
                × Something went wrong:
                   ╭─[test.kdl:33:13]
                33 │ line thirty-three
                   ·             ─────
                34 │ line thirty-four
                   · ────────────────
                35 │ last line (thirty-five)
                   · ────┬────
                   ·     ╰ this is a very long label
                   ╰─
                   """.trimIndent()
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        "test.kdl",
                        listOf(
                            SourceLine(33, "line thirty-three"),
                            SourceLine(34, "line thirty-four"),
                            SourceLine(35, "last line (thirty-five)")
                        ),
                        Span.of(33, 6, 34, 1)
                    ),
                    "this is a very long label"
                ),
                """
                × Something went wrong:
                   ╭─[test.kdl:33:6]
                33 │ line thirty-three
                   ·      ────────────
                34 │ line thirty-four
                   · ┬
                   · ╰ this is a very long label
                35 │ last line (thirty-five)
                   ╰─
                   """.trimIndent()
            ),
            Arguments.of(
                KdlParseException(
                    "Something went wrong",
                    dev.kdl.parse.context.ParseContext(
                        "test.kdl",
                        listOf(
                            SourceLine(
                                737,
                                "line seven hundred and thirty-seven"
                            )
                        ),
                        Span.of(737, 35, 737, 35)
                    ),
                    "this is a very long label"
                ),
                """
                × Something went wrong:
                    ╭─[test.kdl:737:35]
                737 │ line seven hundred and thirty-seven
                    ·                                   ┬
                    ·         this is a very long label ╯
                    ╰─
                    """.trimIndent()
            ),
            Arguments.of(
                KdlHybridParseException(
                    KdlParseException(
                        "this error happened in the KDL v2 parser",
                        dev.kdl.parse.context.ParseContext(
                            "test.kdl",
                            listOf(
                                SourceLine(
                                    737,
                                    "line seven hundred and thirty-seven"
                                )
                            ),
                            Span.of(737, 6, 737, 10)
                        ),
                        "oops",
                        "maybe try with the KDL v1 parser"
                    ),
                    KdlParseException(
                        "this error happened in the KDL v1 parser",
                        dev.kdl.parse.context.ParseContext(
                            "test.kdl",
                            listOf(
                                SourceLine(
                                    33,
                                    "line thirty-three"
                                )
                            ),
                            Span.of(33, 6, 33, 11)
                        ),
                        "oops",
                        "this is hopeless"
                    )
                ),
                """
                Failed to parse the document using both the KDL v2 and the KDL v1 parser.
                KDL v2 error:
                × this error happened in the KDL v2 parser:
                    ╭─[test.kdl:737:6]
                737 │ line seven hundred and thirty-seven
                    ·      ──┬──
                    ·   oops ╯
                    ╰─
                help: maybe try with the KDL v1 parser
                KDL v1 error:
                × this error happened in the KDL v1 parser:
                   ╭─[test.kdl:33:6]
                33 │ line thirty-three
                   ·      ──┬───
                   ·   oops ╯
                   ╰─
                help: this is hopeless
                """.trimIndent()
            )
        )
    }
}
