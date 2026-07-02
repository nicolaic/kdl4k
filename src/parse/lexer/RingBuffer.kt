package dev.kdl.parse.lexer

internal class RingBuffer<T>(capacity: Int) {
    private val items: Array<T?>
    private var head = -1
    private var tail = -1

    init {
        require(capacity >= 1) { "capacity must be a least 1" }

        @Suppress("UNCHECKED_CAST")
        this.items = arrayOfNulls<Any>(capacity) as Array<T?>
    }

    fun capacity(): Int {
        return items.size
    }

    fun size(): Int {
        if (head == -1) {
            return 0
        } else if (head > tail) {
            return capacity() - head + tail + 1
        }
        return tail - head + 1
    }

    val isEmpty: Boolean
        get() = size() == 0

    fun get(index: Int): T? {
        if (index < 0 || index >= size()) {
            throw IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size())
        }
        return items[addToIndex(head, index)]
    }

    fun addFirst(item: T?) {
        checkIsNotFull()
        if (head == -1) {
            head = 0
            tail = 0
        } else {
            head = addToIndex(head, -1)
        }
        items[head] = item
    }

    fun addLast(item: T) {
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

    fun removeFirst(): T {
        checkIsNotEmpty()
        val item = items[head]
        items[head] = null
        if (size() == 1) {
            head = -1
            tail = -1
        } else {
            head = addToIndex(head, 1)
        }
        return item!!
    }

    fun removeLast(): T {
        checkIsNotEmpty()
        val item = items[tail]
        items[tail] = null
        if (size() == 1) {
            head = -1
            tail = -1
        } else {
            tail = addToIndex(tail, -1)
        }
        return item!!
    }

    private fun checkIsNotEmpty() {
        check(size() != 0) { "Ring buffer is empty" }
    }

    private fun addToIndex(index: Int, amount: Int): Int {
        val computedIndex = index + amount
        return if (computedIndex < 0) computedIndex + capacity() else computedIndex % capacity()
    }
}
