package dev.kdl

/**
 * A KDL node.
 *
 * @param type       the type of the node
 * @param name       the name of the node
 * @param arguments  the arguments of the node
 * @param properties the properties of the node
 * @param children   the children of the node
 */
data class KdlNode(
    val name: String,
    val type: String? = null,
    val arguments: List<KdlValue<*>> = emptyList(),
    val properties: KdlProperties = KdlProperties(),
    val children: List<KdlNode> = emptyList(),
) {
    /**
     * Retrieves a property from the node. If a property is defined multiple times, the last value is returned.
     *
     * @param name the name of the property to retrieve
     * @param <T>  the type of the property's value
     * @return an option containing the last value of the property if it has any
    </T> */
    fun <T> getProperty(name: String): KdlValue<T?>? = properties.getValue(name)

    override fun toString(): String = buildString {
        append("KDLNode(").also {
            if (type != null) append('(', type, ')')
        }.append(name)

        arguments.forEach { argument -> append(' ', argument) }
        properties.forEach { property -> append(' ', property) }

        if (children.isNotEmpty()) append('[', children.joinToString(prefix = "[", postfix = "]"), ']')

        append(')')
    }

    /**
     * Creates a new builder to create a new node from the current one.
     *
     * @return a new builder with the type, name, arguments, properties, and children of this document
     */
    fun mutate(): Builder {
        return Builder(type, name, arguments, properties, children)
    }

    /**
     * A [KdlNode] builder.
     */
    class Builder {
        private var type: String? = null
        private var name: String? = null
        private val arguments: MutableList<KdlValue<*>>
        private val properties: KdlProperties.Builder
        private val children: MutableList<KdlNode>

        constructor(
            type: String? = null,
            name: String? = null,
            arguments: List<KdlValue<*>> = emptyList(),
            properties: KdlProperties = KdlProperties(),
            children: List<KdlNode> = emptyList()
        ) {
            this.type = type
            this.name = name
            this.arguments = arguments.toMutableList()
            this.properties = properties.mutate()
            this.children = children.toMutableList()
        }

        /**
         * Sets the name of the node.
         *
         * @param name the name of the node
         * @return `this`
         */
        fun name(name: String): Builder {
            this.name = name
            return this
        }

        /**
         * Sets the type of the node.
         *
         * @param type the type of the node, or `null` if it has no type
         * @return `this`
         */
        fun type(type: String?): Builder {
            this.type = type
            return this
        }

        /**
         * Adds an argument to the node.
         *
         * @param value the value of the argument
         * @return `this`
         */
        fun argument(value: Any?): Builder {
            arguments.add(KdlValue.from(value))
            return this
        }

        /**
         * Adds a typed argument to the node.
         *
         * @param type  the type of the argument
         * @param value the value of the argument
         * @return `this`
         */
        fun argument(value: Any?, type: String?): Builder {
            arguments.add(KdlValue.from(value, type))
            return this
        }

        /**
         * Adds a property to the node.
         *
         * @param name  the name of the property
         * @param value the value of the property
         * @return `this`
         */
        fun property(name: String, value: Any?): Builder {
            properties.property(name, KdlValue.from(value))
            return this
        }

        /**
         * Adds a typed property to the node.
         *
         * @param name  the name of property
         * @param type  the type of the property
         * @param value the value of the property
         * @return `this`
         */
        fun property(name: String, type: String?, value: Any?): Builder {
            properties.property(name, KdlValue.from(value, type))
            return this
        }

        /**
         * Adds a child to the node.
         *
         * @param node the child to add
         * @return `this`
         */
        fun child(node: KdlNode): Builder {
            children.add(node)
            return this
        }

        /**
         * Adds a child to the node.
         *
         * @param node a builder for the child to add
         * @return `this`
         */
        fun child(node: Builder): Builder {
            children.add(node.build())
            return this
        }

        /**
         * Adds children to the node.
         *
         * @param nodes the children to add
         * @return `this`
         */
        fun children(nodes: List<KdlNode>): Builder {
            children.addAll(nodes)
            return this
        }

        /**
         * Adds children to the node.
         *
         * @param nodes the children to add
         * @return `this`
         */
        fun children(vararg nodes: KdlNode): Builder {
            children.addAll(listOf(*nodes))
            return this
        }

        /**
         * Adds children to the node.
         *
         * @param nodes the builders of the children to add
         * @return `this`
         */
        fun children(vararg nodes: Builder): Builder {
            for (node in nodes) {
                children.add(node.build())
            }
            return this
        }

        /**
         * Builds the node.
         *
         * @return the built node
         */
        fun build(): KdlNode {
            return KdlNode(requireNotNull(name), type, arguments, properties.build(), children)
        }
    }

    companion object {
        /**
         * @return a new node builder
         */
        fun builder(): Builder {
            return Builder()
        }
    }
}
