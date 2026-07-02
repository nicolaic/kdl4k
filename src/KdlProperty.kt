package dev.kdl

import kotlin.jvm.JvmRecord

/**
 * A KDL property.
 * 
 * @param name  the name of the property
 * @param value the value of the property
 * @param T     the inner representation of the property's value
 */
@JvmRecord
data class KdlProperty<T>(
    val name: String,
    val value: KdlValue<T?>,
) {
    override fun toString(): String = "$name=$value"
}
