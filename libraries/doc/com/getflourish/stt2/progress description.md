# Description of the process to integrate STT with TarsosDSP and overhaul it in the process


I already had a good idea of the general architecture and mode of operation of STT.
Here's what I did so far:

 0. Decompile STT’s Java bytecode to source code and make that more human-
    readable.

 1. Identify, which code parts are relevant to which function of the library.

 2. Give more descriptive names to a bunch of variables and methods and add a few
    source code comments for my own understanding.

 3. Disengage code parts irrelevant to basic functionality (threshold-based
    activation and threshold detection.

 4. Make the STT class implement TarsosDSP’s AudioProcessor interface. piece
    together from available STT code or re-implement audio stream transcoding
    considering the following aspects:

     * Investigate and use the capabilities of the transcoding library
       "javaFlacEncoder" to transcode from and to audio streams instead of
       intermediate files, leading to more parallelisation and (given enough
       processing power) less delay. It also requires signed 16-bit integer
       samples, requiring a simple conversion of TarsosDSP’s 32-bit floating
       point samples.

     * Investigate restriction on audio formats accepted by Google with the help
     of the [reverse-engineered documentation][1]. Result: Google accepts single-
     channel, signed 16-bit little-endian PCM streams with or without header of
     various sampling rates and single-channel FLAC streams of any sampling rate
     and sampling format.

    This allowed for a much simpler processing chains relying on multiple threads
    following the producer-consumer pattern instead of keeping track of various
    program states and process and store intermediate results from state to state.
    This decreased the degree of integration of the new library with Processing
    while providing interfaces and using patterns more common in plain Java.

 5. Since STT’s HTTP request class was clumsy and jumped through many unnecessary
    hoops for simple HTTP POST requests, use Java's built-in HttpUrlConnection
    instead.

 6. Small improvements in the instrumentation of Gson, a JSON parser library, to
    parse Google’s request result.

 7. Write a unit test for request result parsing and a mock service connector, so
    no actual (remote) network requests are necessary during tests.

 8. Add STT to the list of audio processors of the audio dispatcher.

 9. Debugging.
 
 10. Work around an issue with repeated key events for held down keys.
 
 11. Fix bugs in javaFlacEncoder that prevent concurrent, non-blocking operation.


[1]: https://github.com/gillesdemey/google-speech-v2/
