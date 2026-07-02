package dev.kdl.print

import kotlinx.io.IOException

internal class KdlPrintException(cause: IOException) : RuntimeException(cause) {

    override val cause: IOException = super.cause as IOException
}
