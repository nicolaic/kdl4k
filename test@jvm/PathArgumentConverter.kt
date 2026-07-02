package dev.kdl

import kotlinx.io.files.Path
import org.junit.jupiter.params.converter.SimpleArgumentConverter

class PathArgumentConverter : SimpleArgumentConverter() {

	override fun convert(source: Any?, targetType: Class<*>): Path = when (source) {
		is Path -> source
		is String -> Path(source)
		else -> error("cannot convert from ${source?.javaClass}")
	}
}
