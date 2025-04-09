package com.scepticalphysiologist.dmaple.etc

import java.io.File

/** A path including a possible count and extension.
 *
 */
class PathStruct(
    val name: String,
    val extension: String,
    val count: Int
) {

    val path: File get() = File(if(count > 0) "${name}_$count$extension" else "$name$extension")

    companion object {

        fun fromString(path: String, sep: String = File.pathSeparator): PathStruct {
            // Indexes.
            val n = path.length
            val i = path.lastIndexOf(sep)
            val j = path.lastIndexOf("_")
            val k = path.lastIndexOf(".")

            // Get name, extension and number.
            var name = path
            var numb = 0
            var ext = ""
            if(i < k) {
                name = path.slice(0 until k)
                ext = path.slice(k until n)
            }
            if(i < j) {
                val p = j + 1
                if(p < k) numb = path.slice(p until k).toIntOrNull() ?: 0
                else if(p < n) numb = path.slice(p until n).toIntOrNull() ?: 0
                if(numb != 0) name = path.slice(0 until j)
            }

            return PathStruct(name = name, extension = ext, count = numb)
        }

        fun fromFile(path: File, sep: String = File.pathSeparator): PathStruct {
            return fromString(path.name, sep)
        }

    }

}
