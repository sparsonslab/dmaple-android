package com.scepticalphysiologist.dmaple.map

import android.graphics.Bitmap
import com.scepticalphysiologist.dmaple.etc.Frame
import com.scepticalphysiologist.dmaple.etc.rotateBitmap

/** An image of a mapping field.
 *
 * @property frame The reference frame of the image.
 * @property bitmap The field's image.
 */
class MappingFieldImage(
    var frame: Frame,
    var bitmap: Bitmap
) {

    /** Change the image's reference frame. */
    fun changeFrame(newFrame: Frame) {
        bitmap = rotateBitmap(bitmap, -(newFrame.orientation - frame.orientation))
        frame = newFrame
    }

    /** Get a copy of the image in a new frame. */
    fun inNewFrame(newFrame: Frame): MappingFieldImage {
        val cpy = this.copy()
        cpy.changeFrame(newFrame)
        return cpy
    }

    /** Copy the image. */
    fun copy(): MappingFieldImage {
        return MappingFieldImage(
            frame = this.frame,
            bitmap = this.bitmap
        )
    }
}
