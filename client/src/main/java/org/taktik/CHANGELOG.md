Changes made to mpegts-streamer for ctbrec
=================
* Remove dependency on jcodec
* Add sleepingEnabled flag in Streamer to disable sleeping completely and make processing of stream much faster
* Add sanity check in ContinuityFixer to fix avoid an IndexOutOfBoundsException
* Wait for bufferingThread and streamingThread in Streamer.stream() to make it a blocking method
* Add BlockingMultiMTSSource, which can be used to add sources, after the streaming has been started
* Don't close the stream, if a packet can't be read in one go InputStreamMTSSource. Instead read from
  the stream until the packet is complete
* Remove finalize method. It is deprecated in Java 9. 
