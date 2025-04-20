// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.geom

import com.scepticalphysiologist.dmaple.assertPointsEqual
import com.scepticalphysiologist.dmaple.assertSerializedObjectsEqual
import org.junit.Assert.assertEquals
import org.junit.Test

class RectangleTest {

    @Test
    fun `correct edges`() {
        // Given: A rectangle.
        val rec = Rectangle(c0 = Point(0.1f, 0.5f), c1 = Point(-6.7f, 4.2f))

        // Then: The edge properties are correct.
        assertEquals(-6.7f, rec.left)
        assertEquals(0.1f, rec.right)
        assertEquals(0.5f, rec.top)
        assertEquals(4.2f, rec.bottom)
    }

    @Test
    fun `valid intersection`() {
        // Given: Two rectangles.
        val rec1 = Rectangle(c0 = Point(-5f, 0f), c1 = Point(3f, 4f))
        val rec2 = Rectangle(c0 = Point(1f, -4f), c1 = Point(2f, 2f))

        // When: They are intersected.
        val rec3 = rec1.intersect(rec2)

        // Then: The intersecting rectangle is as expected (serialization used for object comparison).
        val expectedRec3 = Rectangle(c0 = Point(1f, 0f), c1 = Point(2f, 2f))
        assertSerializedObjectsEqual(expectedRec3, rec3)
    }

    @Test
    fun `invalid intersection`() {
        // Given: Two rectangles that do not intersect.
        val rec1 = Rectangle(c0 = Point(-5f, 0f), c1 = Point(3f, 4f))
        val rec2 = Rectangle(c0 = Point(10f, -4f), c1 = Point(4f, -6f))

        // When: They are intersected.
        val rec3 = rec1.intersect(rec2)

        // Then: The output rectangle is null.
        assertEquals(null, rec3)
    }

    @Test
    fun `relative distance calculation`() {
        // Given: A rectangle.
        val rec = Rectangle(c0 = Point(-50f, 50f), Point(50f, -50f))

        // When: The relative distance of various points are calculated.
        // Then: The distance is as expected.
        assertPointsEqual(Point(0f, 0f), rec.relativeDistance(Point(0f, 0f)))
        assertPointsEqual(Point(1f, 1f), rec.relativeDistance(Point(50f, 50f)))
        assertPointsEqual(Point(-1f, -1f), rec.relativeDistance(Point(-50f, -50f)))
        assertPointsEqual(Point(2f, -2f), rec.relativeDistance(Point(100f, -100f)))
        assertPointsEqual(Point(-1.5f, 4.5f), rec.relativeDistance(Point(-75f, 225f)))
    }

    @Test
    fun `translate correctly`() {
        // Given: A rectangle.
        val rec = Rectangle(c0 = Point(-50f, 50f), Point(50f, -50f))

        // When: The rectangle is translated.
        rec.translate(Point(10f, -20f))

        // Then: It is as expected.
        val expectedRec = Rectangle(c0 = Point(-40f, 30f), c1 = Point(60f, -70f))
        assertSerializedObjectsEqual(expectedRec, rec)
    }

}
