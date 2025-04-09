package com.scepticalphysiologist.dmaple.etc

import java.io.File

/** A path (file or directory) that includes a count to indicate the number of
 * existing paths that are otherwise the same.
 *
 * Used for creating unique paths. e.g. If there is an existing path "some/path" and we want to
 * create another path of the same, instead create "some/path_1", then "some/path_2", ... etc.
 *
 * @property body The path up to any count indicator or (for files) its extension.
 * @property extension The file extension (empty for a directory).
 * @property count The number of paths with the same name and extension.
 */
class CountedPath(
    val body: String,
    val extension: String,
    var count: Int
) {

    /** The full path. */
    val path: String get() = if(count > 0) "${body}_$count$extension" else "$body$extension"

    /** The full path as a file object. */
    val file: File get() = File(path)

    companion object {

        /** Parse a path into its [CountedPath]. */
        fun fromString(path: String, sep: String = File.pathSeparator): CountedPath {
            // Indexes.
            val n = path.length
            val i = path.lastIndexOf(sep)
            val j = path.lastIndexOf('_')
            val k = path.lastIndexOf('.')

            // Parse name, extension and count.
            var body = path
            var ext = ""
            var count = 0
            if(i < k) {
                body = path.slice(0 until k)
                ext = path.slice(k until n)
            }
            if(i < j) {
                val p = j + 1
                if(p < k) count = path.slice(p until k).toIntOrNull() ?: 0
                else if(p < n) count = path.slice(p until n).toIntOrNull() ?: 0
                if(count != 0) body = path.slice(0 until j)
            }
            return CountedPath(body = body, extension = ext, count = count)
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
            it.body == this.body && it.extension == this.extension
        }.maxOfOrNull { it.count } ?: -1
        // Make the count for this path one more.
        count = existingCount + 1
    }

}
