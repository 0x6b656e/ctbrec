1.5.3
========================
* Recording time is now converted to local timezone and formatted nicely
* The state is now displayed in the resolution tag, if the room is not
  public (e.g. private, group, offline, away)
* You can now filter for public rooms with the keyword "public", if
  the display of resolution is enabled
* Added possibility to switch between online and offline models in the
  followed tab

1.5.2
========================
* Added possibility to select multiple models in the overview tabs by
  holding SHIFT while clicking
* Added possibility to stop a recording in the recordings tab
* The delete key can now be used in the recorded models tab and in the 
  followed tab to unfollow one ore more models

1.5.1
========================
* Added setting to split up the recording after x minutes 
* Fixed possible OutOfMemoryError, which was caused by invalid transport
  stream packets
* Fixed possible OutOfMemoryError, which could occur, if the stream was
  downloaded faster than it could be written to the hard drive

1.5.0
========================
* Recordings are now stored in a single file
* The server still saves segments, downloads are
  one single file, too
* Recordings and downloads are now proper transport streams
  (continuity and timestamps get fixed, if invalid)

1.4.3
========================
* Added possibility to switch the video resolution for a recording
* Added selection box below the overview pages to change the thumbnail size
* Save and restore window size, location and maximized state
* Added check for OpenJDK and JavaFX on start to print out a better error,
  if JavaFX is not available

1.4.2
========================
* Enabled proxy authentication for SOCKS4 and HTTP
* Empty recording directories are now ignored instead of cluttering the log 
  file with error messages

1.4.1
========================
* Added proxy settings
* Made playlist generator more robust
* Fixed some issues with the file merging
* Fixed memory leak caused by the model filter function