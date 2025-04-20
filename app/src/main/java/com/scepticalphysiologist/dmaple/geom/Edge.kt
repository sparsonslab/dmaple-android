// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.geom

/** An edge of (for example) a [Rectangle]. */
enum class Edge {
    LEFT,
    BOTTOM,
    RIGHT,
    TOP;

    /** Rotate an edge.
     *
     * Edges run clockwise (+rotation) from LEFT > BOTTOM > RIGHT > TOP. This matches their
     * definition in an [android.graphics.Rect] and [Rectangle], where in terms of coordinate space,
     * left < right and top < bottom.
     */
    fun rotate(degrees: Int): Edge {
        var k = (this.ordinal + degrees / 90) % 4
        if (k < 0) k += 4
        return Edge.entries[k]
    }

    fun isVertical(): Boolean {
        return (this == LEFT) || (this == RIGHT)
    }
}
