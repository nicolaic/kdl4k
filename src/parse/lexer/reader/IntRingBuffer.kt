package dev.kdl.parse.lexer.reader

internal class IntRingBuffer(capacity: Int) {
    private val items: IntArray
    private var head = -1
    private var tail = -1

    init {
        require(capacity >= 1) { "capacity must be a least 1" }
        this.items = IntArray(capacity)
    }

    fun capacity(): Int = items.size

    fun size(): Int = when {
        head == -1 -> 0
        head > tail -> capacity() - head + tail + 1
        else -> tail - head + 1
    }

    val isEmpty: Boolean get() = size() == 0

    fun get(index: Int): Int {
        if (index < 0 || index >= size()) {
            throw IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size())
        }

        return items[addToIndex(head, index)]
    }

    fun addFirst(item: Int) {
        checkIsNotFull()

        if (head == -1) {
            head = 0
            tail = 0
        } else {
            head = addToIndex(head, -1)
        }

        items[head] = item
    }

    fun addLast(item: Int) {
        checkIsNotFull()

        if (head == -1) {
            head = 0
            tail = 0
        } else {
            tail = addToIndex(tail, 1)
        }

        items[tail] = item
    }

    private fun checkIsNotFull() {
        check(size() != capacity()) { "Ring buffer is full" }
    }

    fun removeFirst(): Int {
        checkIsNotEmpty()

        val item = items[head]
        if (size() == 1) {
            head = -1
            tail = -1
        } else {
            head = addToIndex(head, 1)
        }

        return item
    }

    fun removeLast(): Int {
        checkIsNotEmpty()

        val item = items[tail]
        if (size() == 1) {
            head = -1
            tail = -1
        } else {
            tail = addToIndex(tail, -1)
        }

        return item
    }

    private fun checkIsNotEmpty() {
        check(size() != 0) { "Ring buffer is empty" }
    }

    private fun addToIndex(index: Int, amount: Int): Int {
        val computedIndex = index + amount

        return if (computedIndex < 0) {
            computedIndex + capacity()
        } else {
            computedIndex % capacity()
        }
    }
}
