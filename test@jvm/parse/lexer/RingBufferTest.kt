package dev.kdl.parse.lexer

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class RingBufferTest {

    @Nested
    @DisplayName("addFirst(T) should")
    internal inner class AddFirst {
        @Test
        @DisplayName("add an item to an empty ring buffer")
        fun emptyRingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)

            ringBuffer.addFirst("Hello, World!")

            assertEquals(1, ringBuffer.size())
            assertEquals("Hello, World!", ringBuffer.get(0))
        }

        @Test
        @DisplayName("add an item to a ring buffer of size 1")
        fun size1RingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addFirst("first item")

            ringBuffer.addFirst("second item")

            assertEquals(2, ringBuffer.size())
            assertEquals("second item", ringBuffer.get(0))
            assertEquals("first item", ringBuffer.get(1))
        }

        @Test
        @DisplayName("throw an IllegalStateException when the buffer is full")
        fun fullRingBuffer() {
            val ringBuffer = RingBuffer<String?>(1)
            ringBuffer.addFirst("item")

            val exception = assertThrows<IllegalStateException> { ringBuffer.addFirst("oops") }
            assertEquals("Ring buffer is full", exception.message)
        }
    }

    @Nested
    @DisplayName("addLast(T) should")
    internal inner class AddLast {
        @Test
        @DisplayName("add an item to an empty ring buffer")
        fun emptyRingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)

            ringBuffer.addLast("Hello, World!")

            assertEquals(1, ringBuffer.size())
            assertEquals("Hello, World!", ringBuffer.get(0))
        }

        @Test
        @DisplayName("add an item to a ring buffer of size 1")
        fun size1RingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addLast("first item")

            ringBuffer.addLast("second item")

            assertEquals(2, ringBuffer.size())
            assertEquals("first item", ringBuffer.get(0))
            assertEquals("second item", ringBuffer.get(1))
        }

        @Test
        @DisplayName("throw an IllegalStateException when the buffer is full")
        fun fullRingBuffer() {
            val ringBuffer = RingBuffer<String?>(1)
            ringBuffer.addLast("item")

            val exception = assertThrows<IllegalStateException> { ringBuffer.addLast("oops") }
            assertEquals("Ring buffer is full", exception.message)
        }
    }

    @Nested
    @DisplayName("get(int) should")
    internal inner class Get {
        @Test
        @DisplayName("return the first item when index is 0 and buffer has one item")
        fun size1RingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addFirst("item")

            val result = ringBuffer.get(0)

            assertEquals("item", result)
        }

        @Test
        @DisplayName("throw an IndexOutOfBoundException when index is negative")
        fun negativeIndex() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addFirst("item")

            val exception = assertThrows<IndexOutOfBoundsException> { ringBuffer.get(-1) }
            assertEquals("Index -1 out of bounds for size 1", exception.message)
        }

        @Test
        @DisplayName("throw an IndexOutOfBoundException when index is equal to the size")
        fun outOfBoundsIndex() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addFirst("item")

            val exception = assertThrows<IndexOutOfBoundsException> { ringBuffer.get(1) }
            assertEquals("Index 1 out of bounds for size 1", exception.message)
        }
    }

    @Nested
    @DisplayName("removeFirst() should")
    internal inner class RemoveFirst {
        @Test
        @DisplayName("remove and return the first item when the buffer has one item")
        fun size1RingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addFirst("item")

            val result = ringBuffer.removeFirst()

            assertEquals(0, ringBuffer.size())
            assertEquals("item", result)
        }

        @Test
        @DisplayName("remove and return the first item when the buffer has two items")
        fun size2RingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addFirst("item1")
            ringBuffer.addLast("item2")

            val result = ringBuffer.removeFirst()

            assertEquals(1, ringBuffer.size())
            assertEquals("item1", result)
        }

        @Test
        @DisplayName("throw an IllegalStateException when the buffer is empty")
        fun emptyRingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)

            val exception = assertThrows<IllegalStateException> { ringBuffer.removeFirst() }
            assertEquals("Ring buffer is empty", exception.message)
        }
    }

    @Nested
    @DisplayName("removeLast() should")
    internal inner class RemoveLast {
        @Test
        @DisplayName("remove and return the last item when the buffer has one item")
        fun size1RingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addFirst("item")

            val result = ringBuffer.removeLast()

            assertEquals(0, ringBuffer.size())
            assertEquals("item", result)
        }

        @Test
        @DisplayName("remove and return the first item when the buffer has two items")
        fun size2RingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)
            ringBuffer.addFirst("item1")
            ringBuffer.addLast("item2")

            val result = ringBuffer.removeLast()

            assertEquals(1, ringBuffer.size())
            assertEquals("item2", result)
        }

        @Test
        @DisplayName("throw an IllegalStateException when the buffer is empty")
        fun emptyRingBuffer() {
            val ringBuffer = RingBuffer<String?>(3)

            val exception = assertThrows<IllegalStateException>(ringBuffer::removeLast)
            assertEquals("Ring buffer is empty", exception.message)
        }
    }
}
