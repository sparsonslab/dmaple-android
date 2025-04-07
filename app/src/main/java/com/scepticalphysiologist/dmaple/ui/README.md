# User Interface

There are only three `Fragment` interfaces in the whole app:

- **[`Recorder`](Recorder.kt)**. Shows the mapping field (camera feed) onto which the user can draw and set-up mapping ROIs
and calibrate the spatial dimensions using a "ruler". When a recording is started, the mapping field retreats
to the top left corner of the screen to reveal a live view of the map(s) as they are created.
- **[`Settings`](Settings.kt)**. For setting preferences - screen presentation, mapping algorithm parameters.
- **[`Explorer`](Explorer.kt)**. A file explorer for selecting previous recordings that can then be loaded onto the Recorder UI.

| Package          | Description                                                                                         |
|------------------|-----------------------------------------------------------------------------------------------------|
| [dialog](dialog) | Dialogs for showing warnings, setting the maps of a mapping ROI, setting the ruler and saving maps. |
| [record](record) | The custom `View`s that make up the `Recorder` fragment.                                            |


## Layout

The [`Explorer`](Explorer.kt) fragment and all the [dialogs](dialog) use Jetpack Compose. Using compose for
the [`Recorder`](Recorder.kt) fragment would (if could be done) result in some seriously nasty-looking code - the
UI is very complex with multiple view layers, gesture detections, screen orientation responses and user interactions. It would
be foolish to try and port this code to Compose! The [`Settings`](Settings.kt) fragment uses Android's built-in preferences framework
for layout.
