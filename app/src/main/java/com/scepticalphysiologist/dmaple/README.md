# Architecture



| Package      | Description                                                                         |
|--------------|-------------------------------------------------------------------------------------|
| [etc](etc)   | Miscellaneous functions and classes.                                                |
| [geom](geom) | Geometric objects (points, rectangles, edges, frames) and transformation.           |
| [map](map)   | Spatio-temporal map calculation, data buffering, read/write and foreground service. |
| [ui](ui)     | Fragments and views.                                                                |


| Term                | Description                                                                                                                                                                                                              |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| diameter map        | An image for visualising the motility of a length of gut. The pixel value at a particular (x, y) location encodes gut diameter at a particular time and distance along rhe length of the gut. See *spatio-temporal map*. |
| mapping field       | The visual field captured by a camera and the objects (ROIs, ruler, etc.) associated with it.                                                                                                                            |
| mapping ROI         | A region-of-interest (ROI) the demarcates the region of a gut to create maps for.                                                                                                                                        |
| spatio-temporal map | An image whose dimensions (width and height) represent time and space. See *diameter map*.                                                                                                                               |
|                     |                                                                                                                                                                                                                          |
|                     |                                                                                                                                                                                                                          |