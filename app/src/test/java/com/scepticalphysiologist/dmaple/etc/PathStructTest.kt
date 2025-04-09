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
            Arguments.of("directory", PathStruct("directory",  "", 0)),
            Arguments.of("some/directory", PathStruct("some/directory",  "", 0)),
            Arguments.of("direct_ory", PathStruct("direct_ory",  "", 0)),
            Arguments.of("direct_45ory", PathStruct("direct_45ory",  "", 0)),
            Arguments.of("directory_12", PathStruct("directory",  "", 12)),
            Arguments.of("some/directory_9", PathStruct("some/directory",  "", 9)),
            Arguments.of("directory/somefile.dat", PathStruct("directory/somefile",  ".dat", 0)),
            Arguments.of("directory/some_file.dat", PathStruct("directory/some_file",  ".dat", 0)),
            Arguments.of("directory/some_file_.dat", PathStruct("directory/some_file_",  ".dat", 0)),
            Arguments.of("directory/some_file_16.jpeg", PathStruct("directory/some_file",  ".jpeg", 16)),
            Arguments.of("directory/somefile_5.tiff", PathStruct("directory/somefile",  ".tiff", 5)),
        )

    }

    @ParameterizedTest(name = "case #{index} ==> {0} = {1}")
    @MethodSource("samplePaths")
    fun `correct structure`(path: String, expectedStruct: PathStruct) {
        // Given: A path and an expected structure.
        // ....

        // When: The the path structure is calculated.
        val struct = PathStruct.fromString(path, "/")

        // Then: The returned structure is as expected.
        assertSerializedObjectsEqual(expectedStruct, struct)
    }

}
