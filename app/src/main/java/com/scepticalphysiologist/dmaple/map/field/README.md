# The Mapping Field

The mapping field is just the area ("field of view") imaged by the camera and the objects the user 
draws on that field, namely:

-  [`FieldRoi`](FieldRoi.kt). Rectangular ROIs the demarcate a length of a particular gut that is to be
mapped. The ROI includes attributes such as the type of maps that are to be created for the length of gut;
the threshold used for determining the gut's diameter and; the "seeding edge", the end of the gut that will
be used to detect the gut and initiate the map.

- [`FieldRuler`](FieldRuler.kt). A ruler used to spatially calibrate the maps. i.e. For the user to
tell the app the size of the field in real units (cm, mm, etc.).

- [`FieldImage`](FieldImage.kt). An image of the field.

These objects all have a [`Frame`](../../geom/Frame.kt) attribute and a `changeFrame()` method. These
allow the objects to be transformed geometrically when the field's view, as it appears the device screen, 
rotates or re-sizes.

[`RoisAndRuler`](RoisAndRuler.kt) is a data class consisting of a field's collection of
[`FieldRoi`](FieldRoi.kt) and its single [`FieldRuler`](FieldRuler.kt). This class is useful as
these objects are passed together through a series of functions between views and the backend.

