package dev.kdl

import dev.kdl.parse.KdlParseException
import dev.kdl.parse.KdlParser
import dev.kdl.parse.Reporter
import dev.kdl.parse.lexer.reader.KdlReadException
import dev.kdl.print.KdlPrinter
import dev.kdl.print.KdlPrinterConfiguration
import dev.kdl.print.KdlPrinterConfiguration.PropertiesOrder.NAME_ASCENDING
import dev.kdl.print.KdlPrinterConfiguration.Whitespace.SPACE
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import org.junit.jupiter.params.provider.Arguments
import kotlin.test.assertEquals
import kotlin.test.fail

abstract class RoundTripTest protected constructor(version: KdlVersion) {
	private val parser: KdlParser = KdlParser.fromVersion(version)
	private val printer: KdlPrinter = KdlPrinter(
		KdlPrinterConfiguration(
			version = version,
			indentation = listOf(SPACE, SPACE, SPACE, SPACE),
			printDuplicateProperties = false,
			propertiesOrder = NAME_ASCENDING
		)
	)

	@Throws(java.io.IOException::class)
	protected fun executeRoundTripTest(
		input: Path,
		expectedOutputPath: Path,
		expectedReportPath: Path,
	) {
		val shouldSucceed = SystemFileSystem.exists(expectedOutputPath)
		try {
			val document = parser.parse(input)
			val output = printer.printToString(document)

			if (!shouldSucceed) {
				fail("Parse exception expected but got: \n$output")
			} else {
				val expectedOutput = SystemFileSystem.source(expectedOutputPath).buffered().readString()
				assertEquals(expectedOutput, output)
			}
		} catch (e: KdlParseException) {
			if (shouldSucceed) {
				fail("Unexpected exception", e)
			}

			if (SystemFileSystem.exists(expectedReportPath)) {
				val report = Reporter.getReport(e)
				val expectedReport = SystemFileSystem.source(expectedReportPath).buffered()
					.readString()

				assertEquals(expectedReport, report)
			}
		} catch (e: KdlReadException) {
			if (shouldSucceed) {
				fail("Unexpected exception", e)
			}
		}
	}

	companion object {

		@JvmStatic
		protected fun getInputs(
			inputFolder: Path,
			expectedFolder: Path
		): List<Arguments> = SystemFileSystem.list(inputFolder)
			.map { input ->
				val expectedOutput = Path(expectedFolder, input.name)
				val expectedReport = Path(expectedFolder, "${input.name}.error")

				Arguments.of(input.name, input, expectedOutput, expectedReport)
			}
	}
}
