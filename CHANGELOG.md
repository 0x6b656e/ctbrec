1.10.1
========================
* Double-click starts playback of recordings
* Refresh of thumbnails can be disabled

1.10.0
========================
* Fix: HMAC authentication didn't work for playing and downloading of a
  recording
* Fix: MyFreeCams model names were case sensitive
* Text input on "Recording"-tab now does auto completion for the site name
* Added menu entry to open the directory of a recording
* Post-processing script is now run outside ot the recordings directory
  Make sure, you use absolute paths
* Added setting to configure the directory structure for recordings
* Split up client and server into separat packages. The server package
  only depends on Java 1.8 and can be run with the 32-bit JRE, too.

1.9.0
========================
* Dropped support for Windows 32 bit
* Include JavaFX, so that ctbrec works with OpenJRE and Java >= 11
* Updated embedded Java versions to 11.0.1
* Added column "Recording" to recorded models tab, which indicates that
  a recording is currently running
* Fix: BongaCams recordings didn't start
* Fix: Unfollow for Cam4 didn't work
* Fix: Post-Processing script couldn't be removed
* A lot of smaller changes under the hood

1.8.0
========================
* Added BongaCams
* Added possibility to suspend the recording for a model. The model stays in
  the list of recorded models, but the actual recording is suspended
* HTTP sessions are restored on startup. This should reduce the number of 
  logins needed (especially for Cam4, BongaCams and CamSoda).
* Server can run now run on OpenJRE
* Added JVM parameter to define the configuration directory
  (``-Dctbrec.config.dir``)
* Improved memory management for MyFreeCams

1.7.0
========================
* Added CamSoda
* Added detection of model name changes for MyFreeCams
* Added setting to define a maximum resolution
* Fixed sorting by date in recordings table

1.6.1
========================
* Fixed UI freeze, which occured for a high number of recorded models
* Added Cam4
* Updated the embedded JRE for the Windows bundles to 8u192

1.6.0
========================
* Added support for multiple cam sites
* Sites can be switched on and off in the settings
* Added MyFreeCams
* Fixed proxy authentication for HTTP and SOCKS

1.5.4
========================
* Lots of little tweaks for the GUI

1.5.3
========================
* Recording time is now converted to local timezone and formatted nicely
* The state is now displayed in the resolution tag, if the room is not
  public (e.g. private, group, offline, away)
* You can now filter for public rooms with the keyword "public", if
  the display of resolution is enabled
* Added possibility to switch between online and offline models in the
  followed tab
* Added possibility to send tips

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