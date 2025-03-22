package com.scepticalphysiologist.dmaple.geom

enum class Edge {
    LEFT,
    BOTTOM,
    RIGHT,
    TOP;

    /** Rotate an edge. */
    fun rotate(degrees: Int): Edge {
        var k = (this.ordinal + degrees / 90) % 4
        if (k < 0) k += 4
        return Edge.entries[k]
    }

    fun isVertical(): Boolean {
        return (this == LEFT) || (this == RIGHT)
    }
}

