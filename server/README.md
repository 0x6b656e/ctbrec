# CTB Server

This is the server part, which is only needed, if you want to run ctbrec in client/server mode

## Requirements
* Java 1.8 (32-bit or 64-bit)

## Installation
* Unpack the zip-File
* Make sure, your Java installation is in the PATH environment variable or replace the call to java
  in the script for your platform with the absolute path to the Java executable, e.g
  
  ``"C:\Program Files\Java\jdk1.8.0_192\bin"`` or
  ``/usr/bin/java``
* Run the server once with script for your platform (server-linux.sh, server-macos.sh, server.bat). This will create the settings file,
  which you can use to configure the server. You can find it here:
  
  Windows: Press <kbd>Windows + r</kbd>, type ``%appdata%\ctbrec``, press OK
  
  Linux: ``~/.config/ctbrec``
  
  macOS: `Library/Preferences/ctbrec` in your user home
* When you are done with the configuration, start the server again
* Start the ctbrec application, go to settings and set the *Record Location* to *Remote* and set *Server* and *Port* to point to your server.
* If you want to restrict access to server you can switch on HMAC authentication by enabling *Require authentication*. The application will
  generate a key and display it in a text field. Copy the line, stop the server, and paste the line into the server config in the first line
  after the `{`. Insert a comma `,` at the end of the line.
* Start the server again. You should now see a line like ``12:58:37.540 [main] INFO  ctbrec.recorder.server.HttpServer - HMAC authentication is enabled``
  in the server log.

## License
CTB Recorder is licensed under the GPLv3. See [LICENSE.txt](https://raw.githubusercontent.com/0xboobface/ctbrec/master/LICENSE.txt).
