## geom: Geometric Objects

The main use of these geometric classes is to encapsulate rectangular regions-of-interest (ROIs) for
mapping and handle their transformation as the user re-sizes them and as they rotate with the device 
(tablet/phone/etc.).

The `Point`, `Rectangle` and `Edge` classes have equivalents in `android.graphics` and 
`java.awt.geom`. The reasons for creating these custom classes are:
- The `android.graphics` classes are stubbed in unit tests and are not shadowed by Robolectric.
Therefore they (and any other classes that require them functionally) cannot be unit tested properly.
- The custom classes can be customised to the needs of the app. e.g. operator overloading for point operations.
- The `java.awt.geom` classes are not present in the Android Java distribution.

