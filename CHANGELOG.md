1.14.0
========================
* Added setting for MFC to ignore the upscaled (960p) stream
* Added event system. You can define to show a notification,
  play a sound or execute a program, when the state of a model
  or recording changes
* Added "follow" menu entry on the Recording tab
* Fix: Recordings change from suspended to recording by their own
  when a thumbnail tab is opened and the model is showing
* Fix: Linux scripts don't work on system where bash isn't the
  default shell

1.13.0
========================
* Added  possibility to open small live previews of online models
  in the Recording tab
* Added setting to toggle "Player Starting" message
* Added possibility to add models by their URL
* Added pause / resume all buttons
* Setting to define the base URL for MFC and CTB
* The paused checkbox are now clickable
* Implemented multi-selection for Recording and Recordings tab
* Fix: Don't throw exceptions for unknown attributes in PlaylistParser
* Fix: Don't do space check, if minimum is set to 0
* Fix: Player not starting when path contains spaces

1.12.1
========================
* Fixed downloads in client / server mode

1.12.0
========================
* Added threshold setting to keep free space on the recording device.
  This is useful, if you don't want to use up all of your storage.
  The free space is also shown on the recordings tab
* Tweaked the download internals a lot. Downloads should not hang
  in RECORDING state without actually recording. Downloads should
  be more robust in general.
* Fixed and improved split recordings
* Improved detection of online state for Cam4 models
* Accelerated the initial loading of the "Recording" tab for many
  Chaturbate models
* Recordings tab now shows smaller size units (Bytes, KiB, MiB, GiB) 

1.11.0
========================
* Added model search function
* Added color settings to change the appearance of the application
* Added setting for the online check interval
* Added setting to define the tab the application opens on start
* Double-click starts playback of recordings
* Refresh of thumbnails can be disabled
* Changed settings are saved immediately (including changes of the
  list of recorded models)

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
