package dev.kdl.parse

import dev.kdl.*
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

internal class Kdl2ParserTest {

	@ParameterizedTest
	@MethodSource("validTestCases")
	fun validParserTest(input: String, expectedDocument: KdlDocument) {
		val document: KdlDocument = PARSER.parse(input, "test.kdl")

		assertEquals(expectedDocument, document)
	}

	@ParameterizedTest
	@MethodSource("errorTestCases")
	fun errorParserTest(input: String, report: String) {
		val exception = assertThrows<KdlParseException> { PARSER.parse(input, "test.kdl") }
		assertEquals(report, Reporter.getReport(exception))
	}

	companion object {
		@JvmStatic
		fun validTestCases(): List<Arguments> = listOf(
			Arguments.of("", KdlDocument([])),
			Arguments.of("node", KdlDocument([KdlNode("node")])),
			Arguments.of("(type) node", KdlDocument([KdlNode("node", "type")])),
			Arguments.of(
				"node arg",
				KdlDocument([KdlNode("node", arguments = [KdlString("arg")])])
			),
			Arguments.of(
				"node 1",
				KdlDocument([KdlNode("node", arguments = [KdlNumber.from(1)])])
			),
			Arguments.of(
				"node #null",
				KdlDocument([KdlNode("node", arguments = [KdlNull()])])
			),
			Arguments.of(
				"node #false",
				KdlDocument([KdlNode("node", arguments = [KdlBoolean(false)])])
			),
			Arguments.of(
				"node (argType) arg",
				KdlDocument(
					[KdlNode("node", arguments = [KdlString("arg", "argType")])]
				)
			),
			Arguments.of(
				"node (argType) 3.5",
				KdlDocument(
					[KdlNode("node", arguments = [KdlNumber.from(3.5, "argType")])]
				)
			),
			Arguments.of(
				"node (argType) #null",
				KdlDocument(
					[KdlNode("node", arguments = [KdlNull("argType")])]
				)
			),
			Arguments.of(
				"node (argType) #true",
				KdlDocument(
					[KdlNode("node", arguments = [KdlBoolean(true, "argType")])]
				)
			),
			Arguments.of(
				"node prop=arg",
				KdlDocument(
					[KdlNode("node", properties = KdlProperties("prop" to [KdlString("arg")]))]
				)
			),
			Arguments.of(
				"node prop=1",
				KdlDocument([KdlNode("node", properties = KdlProperties("prop" to [KdlNumber.from(1)]))])
			),
			Arguments.of(
				"node prop=#null",
				KdlDocument(
					[KdlNode("node", properties = KdlProperties("prop" to [KdlNull()]))]
				)
			),
			Arguments.of(
				"node prop=#false",
				KdlDocument(
					[KdlNode("node", properties = KdlProperties("prop" to [KdlBoolean(false)]))]
				)
			),
			Arguments.of(
				"node prop=(argType) arg",
				KdlDocument(
					[KdlNode("node", properties = KdlProperties("prop" to [KdlString("arg", "argType")]))]
				)
			),
			Arguments.of(
				"node prop=(argType) 3.5",
				KdlDocument(
					[KdlNode("node", properties = KdlProperties("prop" to [KdlNumber.from(3.5, "argType")]))]
				)
			),
			Arguments.of(
				"node prop=(argType) #null",
				KdlDocument(
					[KdlNode("node", properties = KdlProperties("prop" to [KdlNull("argType")]))]
				)
			),
			Arguments.of(
				"node prop=(argType) #true",
				KdlDocument(
					[KdlNode("node", properties = KdlProperties("prop" to [KdlBoolean(true, "argType")]))]
				)
			),
			Arguments.of("node /- arg", KdlDocument([KdlNode("node")])),
			Arguments.of("node /- prop = 1", KdlDocument([KdlNode("node")])),
			Arguments.of("node {}", KdlDocument([KdlNode("node")])),
			Arguments.of(
				"node {\nchild-node\n}",
				KdlDocument([KdlNode("node", children = [KdlNode("child-node")])])
			),
			Arguments.of("node /- {\nchild-node\n}", KdlDocument([KdlNode("node")])),
			Arguments.of(
				"node /- { ignored } {\nchild-node\n}",
				KdlDocument([KdlNode("node", children = [KdlNode("child-node")])])
			),
			Arguments.of(
				"first-node\nsecond-node",
				KdlDocument([KdlNode("first-node"), KdlNode("second-node")]),
			),
			Arguments.of(
				"first-node; second-node",
				KdlDocument([KdlNode("first-node"), KdlNode("second-node")]),
			),
			Arguments.of(
				"first-node {}; second-node",
				KdlDocument([KdlNode("first-node"), KdlNode("second-node")]),
			),
			Arguments.of(
				"node {\nfirst-child; second-child\n}",
				KdlDocument([KdlNode("node", children = [KdlNode("first-child"), KdlNode("second-child")])])
			),
			Arguments.of("/- node1 /- 1.0\nnode2", KdlDocument([KdlNode("node2")]))
		)

		@JvmStatic
		fun errorTestCases(): List<Arguments> = listOf(
			Arguments.of(
				"(type) ",
				"""
                × Missing node name after node type:
                  ╭─[test.kdl:1:8]
                1 │ (type)
                  ·        ┬
                  ·        ╰ string expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"() node",
				"""
                × Missing type name in type annotation:
                  ╭─[test.kdl:1:2]
                1 │ () node
                  ·  ┬
                  ·  ╰ string expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"(",
				"""
                × Missing type name in type annotation:
                  ╭─[test.kdl:1:2]
                1 │ (
                  ·  ┬
                  ·  ╰ string expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"(type node",
				"""
                × Missing closing parentheses in type annotation:
                  ╭─[test.kdl:1:1]
                1 │ (type node
                  · ──┬──
                  ·   ╰ closing parentheses expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"node (argType) ",
				"""
                × Missing value after argument type:
                  ╭─[test.kdl:1:16]
                1 │ node (argType)
                  ·                ┬
                  ·                ╰ value expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"node (argType) {}",
				"""
                × Missing value after argument type:
                  ╭─[test.kdl:1:16]
                1 │ node (argType) {}
                  ·                ┬
                  ·                ╰ value expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"node prop = ",
				"""
                × Missing property value:
                  ╭─[test.kdl:1:13]
                1 │ node prop =
                  ·             ┬
                  ·             ╰ value expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"node prop = {}",
				"""
                × Missing property value:
                  ╭─[test.kdl:1:13]
                1 │ node prop = {}
                  ·             ┬
                  ·             ╰ value expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"node { child",
				"""
                × Missing closing brace at the end of children list:
                  ╭─[test.kdl:1:13]
                1 │ node { child
                  ·             ┬
                  ·             ╰ closing brace expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"node /- { child",
				"""
                × Missing closing brace at the end of children list:
                  ╭─[test.kdl:1:16]
                1 │ node /- { child
                  ·                ┬
                  ·                ╰ closing brace expected
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"node { child } { more-children }",
				"""
                × More than one list of children provided for node:
                  ╭─[test.kdl:1:16]
                1 │ node { child } { more-children }
                  ·                ┬
                  ·                ╰ second children list
                  ╰─
                  """.trimIndent()
			),
			Arguments.of(
				"first-node {} second-node",
				"""
                × Semi-colon expected between nodes on the same line:
                  ╭─[test.kdl:1:15]
                1 │ first-node {} second-node
                  ·               ┬
                  ·               ╰ semi-colon expected
                  ╰─
                help: nodes need to be separated by a semi-colon or a newline character
                """.trimIndent()
			),
			Arguments.of(
				"node {\n    first-child {} second-child\n}",
				"""
                × Semi-colon expected between nodes on the same line:
                  ╭─[test.kdl:2:20]
                2 │     first-child {} second-child
                  ·                    ┬
                  ·                    ╰ semi-colon expected
                  ╰─
                help: nodes need to be separated by a semi-colon or a newline character
                """.trimIndent()
			),
			Arguments.of(
				"node {\n    child1\n    /-\n}",
				"""
                × Valid node expected after slashdash:
                  ╭─[test.kdl:4:1]
                3 │     /-
                4 │ }
                  · ┬
                  · ╰ node expected
                  ╰─
                  """.trimIndent()
			)
		)

		private val PARSER = Kdl2Parser()
	}
}
