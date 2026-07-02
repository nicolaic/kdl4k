package dev.kdl

import kotlinx.io.files.Path
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class Kdl1ParserRoundTripTest : RoundTripTest(KdlVersion.V1) {

    @ParameterizedTest(name = "{0}")
    @MethodSource("inputs")
    fun roundTripTest(
		@ConvertWith(PathArgumentConverter::class) filename: Path,
	    @ConvertWith(PathArgumentConverter::class) input: Path,
	    @ConvertWith(PathArgumentConverter::class) expectedOutput: Path,
	    @ConvertWith(PathArgumentConverter::class) expectedReport: Path,
    ) = executeRoundTripTest(input, expectedOutput, expectedReport)

    companion object {
        @JvmStatic
        fun inputs(): List<Arguments> = getInputs(INPUT_FOLDER, EXPECTED_FOLDER)

        private val INPUT_FOLDER = Path("testResources/test-cases/v1/input")
        private val EXPECTED_FOLDER = Path("testResources/test-cases/v1/expected")
    }
}
