package dev.kdl.parse.lexer.reader

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

internal class IntRingBufferTest {
    @Nested
    @DisplayName("addFirst(T) should")
    internal inner class AddFirst {
        @Test
        @DisplayName("add an item to an empty ring buffer")
        fun emptyRingBuffer() {
            val ringBuffer = IntRingBuffer(3)

            ringBuffer.addFirst(1)

            assertEquals(1, ringBuffer.size())
            assertEquals(1, ringBuffer.get(0))
        }

        @Test
        @DisplayName("add an item to a ring buffer of size 1")
        fun size1RingBuffer() {
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addFirst(1)

            ringBuffer.addFirst(2)

            assertEquals(2, ringBuffer.size())
            assertEquals(2, ringBuffer.get(0))
            assertEquals(1, ringBuffer.get(1))
        }

        @Test
        @DisplayName("throw an IllegalStateException when the buffer is full")
        fun fullRingBuffer() {
            val ringBuffer = IntRingBuffer(1)
            ringBuffer.addFirst(1)

            val exception = assertThrows<IllegalStateException> { ringBuffer.addFirst(2) }
            assertEquals("Ring buffer is full", exception.message)
        }
    }

    @Nested
    @DisplayName("addLast(T) should")
    internal inner class AddLast {
        @Test
        @DisplayName("add an item to an empty ring buffer")
        fun emptyRingBuffer() {
            val ringBuffer = IntRingBuffer(3)

            ringBuffer.addLast(1)

            assertEquals(1, ringBuffer.size())
            assertEquals(1, ringBuffer.get(0))
        }

        @Test
        @DisplayName("add an item to a ring buffer of size 1")
        fun size1RingBuffer() {
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addLast(1)

            ringBuffer.addLast(2)

            assertEquals(2, ringBuffer.size())
            assertEquals(1, ringBuffer.get(0))
            assertEquals(2, ringBuffer.get(1))
        }

        @Test
        @DisplayName("throw an IllegalStateException when the buffer is full")
        fun fullRingBuffer() {
            val ringBuffer = IntRingBuffer(1)
            ringBuffer.addLast(1)

            val exception = assertThrows<IllegalStateException> { ringBuffer.addLast(2) }
            assertEquals("Ring buffer is full", exception.message)
        }
    }

    @Nested
    @DisplayName("get(int) should")
    internal inner class Get {
        @Test
        @DisplayName("return the first item when index is 0 and buffer has one item")
        fun size1RingBuffer() {
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addFirst(1)

            val result = ringBuffer.get(0)

            assertEquals(1, result)
        }

        @Test
        @DisplayName("throw an IndexOutOfBoundException when index is negative")
        fun negativeIndex() {
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addFirst(1)

            val exception = assertThrows<IndexOutOfBoundsException> { ringBuffer.get(-1) }
            assertEquals("Index -1 out of bounds for size 1", exception.message)
        }

        @Test
        @DisplayName("throw an IndexOutOfBoundException when index is equal to the size")
        fun outOfBoundsIndex() {
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addFirst(1)

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
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addFirst(1)

            val result = ringBuffer.removeFirst()

            assertEquals(0, ringBuffer.size())
            assertEquals(1, result)
        }

        @Test
        @DisplayName("remove and return the first item when the buffer has two items")
        fun size2RingBuffer() {
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addFirst(1)
            ringBuffer.addLast(2)

            val result = ringBuffer.removeFirst()

            assertEquals(1, ringBuffer.size())
            assertEquals(1, result)
        }

        @Test
        @DisplayName("throw an IllegalStateException when the buffer is empty")
        fun emptyRingBuffer() {
            val ringBuffer = IntRingBuffer(3)

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
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addFirst(1)

            val result = ringBuffer.removeLast()

            assertEquals(0, ringBuffer.size())
            assertEquals(1, result)
        }

        @Test
        @DisplayName("remove and return the first item when the buffer has two items")
        fun size2RingBuffer() {
            val ringBuffer = IntRingBuffer(3)
            ringBuffer.addFirst(1)
            ringBuffer.addLast(2)

            val result = ringBuffer.removeLast()

            assertEquals(1, ringBuffer.size())
            assertEquals(2, result)
        }

        @Test
        @DisplayName("throw an IllegalStateException when the buffer is empty")
        fun emptyRingBuffer() {
            val ringBuffer = IntRingBuffer(3)

            val exception = assertThrows<IllegalStateException> { ringBuffer.removeLast() }
            assertEquals("Ring buffer is empty", exception.message)
        }
    }
}
