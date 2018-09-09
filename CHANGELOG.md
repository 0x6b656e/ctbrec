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