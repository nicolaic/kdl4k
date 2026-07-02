package dev.kdl

/**
 * The properties of a [KdlNode].
 */
data class KdlProperties(
    private val properties: Map<String, List<KdlValue<*>>>
) : Iterable<Map.Entry<String, List<KdlValue<*>>>> {
    constructor(vararg properties: Pair<String, List<KdlValue<*>>>) : this(properties.toMap())

    /**
     * Retrieves the value of a property. If multiple values are defined for the property, the last one is returned.
     *
     * @param property the name of the property to retrieve
     * @param <T>      the type of the property's value
     * @return an option containing the last value of the property if it has any
    </T> */
    @Suppress("UNCHECKED_CAST")
    fun <T> getValue(property: String): KdlValue<T?>? =
        properties[property]?.lastOrNull() as KdlValue<T?>?

    /**
     * Retrieves all the values of a property. Returns an empty list if the property is missing.
     *
     * @param property the name of the property to retrieve
     * @return a list containing all the values of the property
     */
    fun getValues(property: String): List<KdlValue<*>> = properties[property] ?: emptyList()

    /**
     * Checks if a property is present.
     *
     * @param property the name of the property to check
     * @return `true` if the property is present, `false` otherwise
     */
    fun hasProperty(property: String): Boolean = properties.containsKey(property)

    override fun iterator(): Iterator<Map.Entry<String, List<KdlValue<*>>>> = properties.entries.iterator()

    /**
     * @return a set containing the names of all the properties
     */
    fun propertyNames(): Set<String> = properties.keys

    /**
     * Creates a new builder to create new properties from the current ones.
     *
     * @return a new builder with the current properties
     */
    fun mutate(): Builder {
        return Builder().also { builder ->
            properties.forEach { (property, values) ->
                values.forEach { value ->
                    builder.property(property, value)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as KdlProperties

        return properties == other.properties
    }

    override fun hashCode(): Int {
        return properties.hashCode()
    }

    /**
     * A builder for [KdlProperties].
     */
    class Builder {
        private val properties: MutableMap<String, MutableList<KdlValue<*>>> = mutableMapOf()

        /**
         * Adds a property.
         *
         * @param name  the name of the property
         * @param value the value of the property
         * @return `this`
         */
        fun property(name: String, value: KdlValue<*>): Builder {
            properties.getOrPut(name, ::mutableListOf).add(value)
            return this
        }

        /**
         * Builds the properties
         *
         * @return the build properties
         */
        fun build(): KdlProperties {
            return KdlProperties(properties)
        }
    }

    companion object {
        /**
         * @return a new [KdlProperties] builder.
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
