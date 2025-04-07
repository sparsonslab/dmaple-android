# Mapping

This package is the "backend" of the app. 

The keystone class is [`MappingService`](MappingService.kt), a foreground service that runs the recording
of maps and the loading of previous recordings. The reason for using a foreground service is so that once 
started by the user, recording will not stop if the app goes into the background or the screen goes to sleep. In
such cases the Android OS will typically kill the app's activities within a few minutes of backgrounding or sleep, but
the foreground service will continue. Thus when the user presses start they can be assured that recording will 
not stop until they actually press stop. This is vital as recording may be in the order of tens-of-minutes to an hour.

The remaining classes are used for calculation of maps ([creator](creator)), buffering of map data ([buffer](buffer)),
map i/o ([record](record)) and encapsulation of objects in the mapping field ([field](field)).

| Package            | Description                                                                                                         |
|--------------------|---------------------------------------------------------------------------------------------------------------------|
| [buffer](buffer)   | Buffers for collecting map values as they are calculated during recording and for loading of already recorded maps. |
| [creator](creator) | The objects that create (calculate) spatio-temporal maps.                                                           |
| [field](field)     | Objects in the mapping field - mapping ROIs, the ruler and an image of the field itself.                            |
| [record](record)   | The object for encapsulating a recording of maps.                                                                   |

