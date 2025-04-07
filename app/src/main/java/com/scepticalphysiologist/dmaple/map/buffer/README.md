# Buffering

Spatio-temporal maps can get very large - in the region of tens or even hundreds of MB for recordings
of tens of minutes to hours. And the user might want to create several maps simultaneously during
one recording. However Android puts strict limits on the amount of memory any one app can use (typically 256 MB)
and even without such a limit, using this amount of memory would slow down the rest of the app, its UI, etc.

Therefore the app creates several binary files of 100 MB size in the app's private storage and each of these
files is "memory mapped", via a `FileChannel`, to a `ByteBuffer` - specifically a `MappedByteBuffer`. In this way each file
acts as a block of "virtual memory" that can be randomly accessed just like real memory. Each file/buffer is
used to collect data for a single spatio-temporal map (100 MB can hold a typical map of ~1 hour).

[`MapBufferProvider`](MapBufferProvider.kt) creates the buffer files (if they do not already exist), provides
the `MappedByteBuffer` objects as they are required, and keeps track of how many files/buffers are free (have 
not been allocated for a map).

The `MappedByteBuffer` buffers are "wrapped" in a "view" to provide read/write for specific data types, much in the
way the Java provides "views" for the `ByteBuffer` class (`ShortBuffer`, `FloatBuffer`, etc.). [`MapBufferView`](MapBufferView.kt) 
is an abstract parent class for two concrete views:

- [`ShortMap`](ShortMap.kt). Short (16 bit) values of diameters and radii (diameter and radius maps).
- [`RGBMap`](RGBMap.kt). RGB colors of the camera along the gut spine (spine maps).

[`MapBufferView`](MapBufferView.kt) also has methods for writing the map data into a TIFF directory
and loading a map from a TIFF directory. These methods also avoid putting any data into real memory by file
channels, and makes saving and loading of maps relatively quick.

### References

https://developer.android.com/reference/java/nio/MappedByteBuffer

https://developer.android.com/topic/performance/memory-overview

https://medium.com/globant/memory-mapped-files-and-mappedbytebuffers-in-java-4e5819605b20

https://www.tothenew.com/blog/handling-large-files-using-javanio-mappedbytebuffer/

https://jobcardsystems.com/index.php/blog/29-effective-handling-of-big-images-in-java

https://www.happycoders.eu/java/filechannel-memory-mapped-io-locks/

