package com.scepticalphysiologist.dmaple.etc

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import java.io.File
import java.io.InputStream

// -------------------------------------------------------------------------------------------------
// JSON
// -------------------------------------------------------------------------------------------------

/** Read a JSON file. */
fun readJSON(file: File): Map<String, Any?> {
    return replaceMapNan(
        map = (Parser().parse(file.absolutePath) as JsonObject).map,
        toString = false
    )
}

/** Read a JSON file from a stream. */
fun readJSONfrommStream(stream: InputStream): Map<String, Any?> {
    return replaceMapNan(
        map = (Parser().parse(stream) as JsonObject).map,
        toString = false
    )
}

/** Write a JSON file. */
fun writeJSON(file: File, contents: Map<String, Any?>, prettyPrint: Boolean = true) {
    val obj = JsonObject(replaceMapNan(contents.toMutableMap(), toString = true))
    file.writeText(obj.toJsonString(prettyPrint = prettyPrint, canonical = false))
}

/** Replace any NaN values in a (possibly nested) map with "nan" or vise-versa.
 *
 * This is needed to represent NaN in JSON - NaN is not part of the JSON specification.
 * klaxon writes NaN as unquoted NaN, which makes the JSON out-of-specification. We still
 * have to replace Float/Double.NaN with "nan" rather than "NaN" because klaxon still
 * converts the latter to unquoted NaN !!
 *
 * @param map The map.
 * @param toString Replace numeric NaN with string "nan". Otherwise vise-versa.
 * */
fun replaceMapNan(map: MutableMap<String, Any?>, toString: Boolean): MutableMap<String, Any?> {
    for((k, v) in map) map[k] = replaceNan(v, toString)
    return map
}

/** Replace any NaN values with "nan" or vise-versa.
 *
 * @param value The value.
 * @param toString Replace numeric NaN with string "nan". Otherwise vise-versa.
 * */
fun replaceNan(value: Any?, toString: Boolean): Any? {
    if(toString && ((value is Float) && value.isNaN()) || ((value is Double) && value.isNaN())) return "nan"
    if(!toString && (value is String) && (value == "nan")) return Float.NaN
    else if (value is Map<*, *>)  return value.map{it.key to replaceNan(it.value, toString)}.toMap()
    return value
}
