# Architecture

Each package contains a read-me that gives an overview its contents and the architecture defined by
its classes. The read-mes also allow the use of images and tables, which cannot be included in class
docstrings.

| Package      | Description                                                                                                                                                                                                          |
|--------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [etc](etc)   | Miscellaneous functions and classes.                                                                                                                                                                                 |
| [geom](geom) | Geometric objects (points, rectangles, edges, frames) and transformation. Geometry is incredibly important throughout the app, but the built-in geometry classes in `android.graphics` aren't always up to the task. |
| [map](map)   | The "backend" of the app. Spatio-temporal map calculation, data buffering, read/write and foreground service.                                                                                                        |
| [ui](ui)     | The "frontend" of the app. Fragments and views.                                                                                                                                                                      |


## Dictionary

There is a fair amount of domain jargon and architectural lingo which is used throughout the codebase and the
developer should be familiar with.

| Term                | Definition                                                                                                                                                                                                               |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| diameter map        | An image for visualising the motility of a length of gut. The pixel value at a particular (x, y) location encodes gut diameter at a particular time and distance along rhe length of the gut. See *spatio-temporal map*. |
| mapping field       | The visual field captured by a camera and the objects (ROIs, ruler, etc.) associated with it.                                                                                                                            |
| mapping ROI         | A region-of-interest (ROI) the demarcates the region of a gut to create maps for.                                                                                                                                        |
| spatio-temporal map | An image whose dimensions (width and height) represent time and space. See *diameter map*.                                                                                                                               |
|                     |                                                                                                                                                                                                                          |
|                     |                                                                                                                                                                                                                          |

