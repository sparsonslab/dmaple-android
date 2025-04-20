// Copyright (c) 2025-2025. Dr Sean Paul Parsons. All rights reserved.

package com.scepticalphysiologist.dmaple.etc

import com.scepticalphysiologist.dmaple.assertSerializedObjectsEqual
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PathStructTest {

    companion object {

        @JvmStatic
        fun samplePaths() = Stream.of(
            Arguments.of("directory", CountedPath("directory",  "", 0)),
            Arguments.of("some/directory", CountedPath("some/directory",  "", 0)),
            Arguments.of("direct_ory", CountedPath("direct_ory",  "", 0)),
            Arguments.of("direct_45ory", CountedPath("direct_45ory",  "", 0)),
            Arguments.of("directory_12", CountedPath("directory",  "", 12)),
            Arguments.of("some/directory_9", CountedPath("some/directory",  "", 9)),
            Arguments.of("directory/somefile.dat", CountedPath("directory/somefile",  ".dat", 0)),
            Arguments.of("directory/some_file.dat", CountedPath("directory/some_file",  ".dat", 0)),
            Arguments.of("directory/some_file_.dat", CountedPath("directory/some_file_",  ".dat", 0)),
            Arguments.of("directory/some_file_16.jpeg", CountedPath("directory/some_file",  ".jpeg", 16)),
            Arguments.of("directory/somefile_5.tiff", CountedPath("directory/somefile",  ".tiff", 5)),
        )

    }

    @ParameterizedTest(name = "case #{index} ==> {0} = {1}")
    @MethodSource("samplePaths")
    fun `correct structure`(path: String, expectedStruct: CountedPath) {
        // Given: A path and an expected structure.
        // ....

        // When: The the path structure is calculated.
        val struct = CountedPath.fromString(path, "/")

        // Then: The returned structure is as expected.
        assertSerializedObjectsEqual(expectedStruct, struct)
    }

}
