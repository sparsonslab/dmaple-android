package com.scepticalphysiologist.dmaple.etc

import java.io.File

/** A path (file or directory) that includes a count to indicate the number of
 * existing paths that are otherwise the same.
 *
 * Used for creating unique paths. e.g. If there is an existing path "some/path" and it is
 * wanted to create another path of the same name, instead create "some/path_1", then
 * "some/path_2", ... etc.
 *
 * @property name The path up to any count indicator or (for files) its extension.
 * @property extension The file extension (empty for a directory).
 * @property count The number of paths with the same name and extension.
 */
class CountedPath(
    val name: String,
    val extension: String,
    var count: Int
) {

    val path: File get() = File(if(count > 0) "${name}_$count$extension" else "$name$extension")

    companion object {

        /** Parse a path into its [CountedPath]. */
        fun fromString(path: String, sep: String = File.pathSeparator): CountedPath {
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
            return CountedPath(name = name, extension = ext, count = numb)
        }

        /** Parse a path into its [CountedPath]. */
        fun fromFile(file: File, sep: String = File.pathSeparator): CountedPath {
            return fromString(file.path, sep)
        }

    }

    /** Given a list of existing paths, set the count of this path. */
    fun setValidCount(existingPaths: List<String>) {
        // The largest count for all paths that match this one.
        val existingCount = existingPaths.map{fromString(it)}.filter{
            it.name == this.name && it.extension == this.extension
        }.maxOfOrNull { it.count } ?: -1
        // Make the count for this path one more.
        count = existingCount + 1
    }

}
