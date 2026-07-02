package dev.kdl.print

import dev.kdl.KdlDocument
import dev.kdl.KdlNode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class KdlPrinterTest {

    @Test
    @DisplayName("printToString should return a string with the textual representation of the document")
    fun printToString() {
        val document = KdlDocument(listOf(KdlNode("test")))
        val printer = KdlPrinter.v2

        val result = printer.printToString(document)

        assertEquals("test\n", result)
    }
}
