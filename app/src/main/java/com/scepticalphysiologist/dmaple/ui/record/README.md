# Views of the Camera Field and Spatio-temporal Map

These classes make up the view of the [`Recorder`](../Recorder.kt) fragment. They form overlapping 
layers of a single frame layout, from top to bottom:

* [`FieldView`](FieldView.kt). The field-of-view of the camera and annotations drawn onto it.
  - [`SpineOverlay`](SpineOverlay.kt). Lines drawn along the "spine" of gut(s) while they are being mapped.
  - [`ROIOverlay`](RoiOverlay.kt). Rectangular regions-of-interest (ROIs) drawn by the user to indicate which
  guts are to be mapped and where.
  - `Preview` (`androidx.camera.view.PreviewView`). The camera feed.
* [`MapView`](MapView.kt). A spatio-temporal map, selected by tapping one of the ROIs.

|   ![image](../../images/mapfield.jpg)   |
|:---------------------------------------:|
| *View layers of the Recorder fragment.* |


When recording or viewing an old recording, the `FieldView` takes up the top-left of the fragment,
over the top of the`MapView`, and can be resized by dragging its lower-right corner. Otherwise, when not
recording, the `FieldView` takes up the whole fragment and the `MapView` is hidden.




## MapView

### Calculation of the map view port and conversion into a bitmap

To create a bitmap of a "view port" of the spatio-temporal map, `MapView` has to convert bitmap 
coordinates into map coordinates. The linear equation for this is,

$$  m = M_{orig} - M_{off} + bR_{mb} $$

$$ R_{mb} = \frac{R_{mb}^*}{Z} $$

$$ R_{mb}^* = \frac{m_{ext}(x)}{b_{ext}(x)}  $$

The spatial zoom is constrained as,

$$ Z(x) \ge 1 $$

so that the map's view port always takes up the full extent of the view.

$ M_{off}$ is used to scroll the image. It is constrained as,

$$  0 \le M_{off} \le M_{orig} $$

where $M_{off} = 0$ is the map scrolled to its leading edge and $M_{off} = M_{orig}$ is the map
scrolled to its start.

The map viewport is then:

$$ max((0, 0), M_{orig} - M_{off}) : M_{orig} - M_{off} + b_{ext} R_{mb}$$


| symbol     | description                                          | `MapView` attribute      |
|------------|------------------------------------------------------|--------------------------|
| $x$        | space coordinate                                     | -                        |
| $y$        | time coordinate                                      | -                        |
| $b$        | bitmap $(x, y)$ coordinate                           | -                        |
| $m$        | map $(x, y)$ coordinate                              | -                        |
| $b_{ext}$  | the size of the bitmap if it takes up the whole view | `bitmapExtent`           |
| $m_{ext}$  | the size of the map                                  | `mapExtent`              |
| $M_{orig}$ |                                                      | `mapOrigin`              |
| $M_{off}$  | the offset of the view port from its leading edge    | `mapOffset`              |
| $R_{mb}$   | the ratio of map to bitmap pixels                    | `mapBitmapRatio`         |
| $R_{mb}^*$ | the ratio of map to bitmap pixels at zoom of one     | `unitZoomMapBitmapRatio` |
| $Z$        | zoom                                                 | `zoom`                   |

| ![image](../../images/mapview_geom.jpg) |
|:---------------------------------------:|
| *Bitmap to map coordinate conversion.*  |


| ![image](../../images/mapview_calls.jpg) |
|:----------------------------------------:|
|  *Call sequence when updating MapView.*  |



