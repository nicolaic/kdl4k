package dev.kdl

import kotlin.jvm.JvmRecord

/**
 * Supertype for all KDL values.
 *
 * @param T the inner representation of the value
 */
sealed interface KdlValue<T> {
    /**
     * @return the type of the value or `null` if there is no type
     */
    val type: String?

    /**
     * @return the value
     */
    val value: T

    /**
     * @return whether this value is a KDL string
     */
    val isString: Boolean get() = false

    /**
     * @return whether this value is a KDL number
     */
    val isNumber: Boolean get() = false

    /**
     * @return whether this value is a KDL boolean
     */
    val isBoolean: Boolean get() = false

    /**
     * @return whether this value is a KDL null
     */
    val isNull: Boolean get() = false

    companion object {
        /**
         * Creates a new KDL value from its representation.
         *
         * @param type  the type of the value
         * @param value the value to wrap in a [KdlValue]
         * @return a corresponding [KdlValue]
         */
        fun from(value: Any?, type: String? = null): KdlValue<*> = when (value) {
            null -> KdlNull(type)
            is KdlValue<*> -> value
            is Boolean -> KdlBoolean(value, type)
            is Number -> KdlNumber.from(value, type)
            is String -> KdlString(value, type)
            else -> throw IllegalArgumentException("Could not convert $value to a KDL value")
        }
    }
}

/**
 * The KDL null value.
 *
 * @param type the type of the value
 */
@JvmRecord
data class KdlNull(
    override val type: String? = null,
) : KdlValue<Any?> {
    override val isNull: Boolean get() = true
    override val value: Any? get() = null
}

/**
 * A KDL boolean value.
 *
 * @param type  the type of the value
 * @param value the value
 */
@JvmRecord
data class KdlBoolean(
    override val value: Boolean,
    override val type: String? = null,
) : KdlValue<Boolean?> {
    override val isBoolean: Boolean get() = true
}

/**
 * A KDL string value.
 *
 * @param type  the type of the value
 * @param value the value
 */
@JvmRecord
data class KdlString(
    override val value: String,
    override val type: String? = null,
) : KdlValue<String> {
    override val isString: Boolean get() = true
    override fun toString(): String = "String($value)"
}
