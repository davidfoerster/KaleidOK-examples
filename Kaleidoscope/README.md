# Kaleidoscope

## Setup

 1. [Request a Flickr API key](https://www.flickr.com/services/api/keys/apply/)
    and consult the [Configuration] section on where to put it for Kaleidoscope
    to find.

 2. Similarly [request a Google API access key](https://www.chromium.org/developers/how-tos/api-keys)
    and provide it to Kaleidoscope.

 2. Prepare a [configuration] for Kaleidoscope.
 3. Some libraries need to be set up, but I'm not going into that now.


## Usage

 1. Run KaleidoscopeApp as stand-alone Java application or use the included run
    configuration.

 2. Press <kbd>I</kbd> to start recording from the default microphone line and
    <kbd>O</kbd> to stop. The speech in recorded section is transcribed, the
    resulting text synesthetised and the synesthetiation result used to search
    for images with Chromatik.

 3. If you're unwilling or unable to speak to Kaleidoscope through a microphone,
    use the  two text fields below the sketch canvas are. The upper one is for a
    message to synesthetise, the lower one for search terms for Chromatik's image
    search. You can run perform a synesthetiation and image search by having the
    upper text field in focus and pressing <kbd>ENTER</kbd>/<kbd>RETURN</kbd>.

 4. <kbd>F11</kbd> toggles full screen display of Kaleidoscope.


## Configuration

The most convenient way to configure Kaleidoscope is through a properties file:

 1. Create or place a properties file `KaleidoscopeApp.properties` inside the
    package `kaleidok.examples.kaleidoscope` (inside the `src` directory). You
    can use `KaleidoscopeApp.template.properties`, which you can find in the
    same directory as this `README` file, as a template.

 2. Edit the properties files with any text editor. Most importantly, add the
    required API keys (see below).
Kaleidoscope knows a few configuration options, that can be set through applet
parameters. See the included run configuration for examples. Below is a
description of some currently understood options. All parameters except API keys
are optional and have sensible defaults.

 * `com.flickr.api.key`, `com.google.developer.api.key`

    API access keys for Flickr and Google respectively. If the key is comprised of multiple parts, separate them with a colon (`:`). Example:

        e0b92403f258c35c6b43d2e21c640f9f:bd7a0f0bcc5dfc25

    If the value to of these parameters start with `@`, the remainder is
    interpreted as the path to a resource file containing the key, where multiple
    parts are separated by newline characters.

        e0b92403f258c35c6b43d2e21c640f9f
        bd7a0f0bcc5dfc25

    The special parameter value `!MOCK` results in a mock implementation of the
    service connectors, that doesn't use the actual service, but returns a valid,
    pre-recorded result.

    Note: The above example does not contain a working key, just a randomly
    generated look-alike.

 * `kaleidok.util.debugmanager.verbose` –
   Verbosity level

 * `kaleidok.util.debugmanager.wireframe` –
   Draw wireframe models instead of filled and textured surfaces.

 * `kaleidok.examples.kaleidoscope.text` –
   Default text for the upper text input field (see point 3 under *Usage*)

 * `screen` and `fullscreen]` –
   Move the Kaleidoscope window to the screen with the given index (-1 means
   default) and/or put it in fullscreen mode.

 * `kaleidok.examples.kaleidoscope.audio.[samplerate|buffersize|overlap]` –
   Audio sample rate (for microphone input) in Hertz, sample buffer length, and
   buffer overlap

 * `kaleidok.examples.kaleidoscope.audio.input` –
   Uses the specified file (if any) as audio input in a continuous loop instead of
   a microphone line; the sample rate is always that of the audio file.

 * `kaleidok.google.speech.stt.interval` –
   Sets the maximum duration of a speech record in milliseconds to send to the 
   speech-to-text service. Google's web service is supposed to handle 10–15 
   seconds, but a safer value seems to be around 8 seconds.
 
 * `kaleidok.google.speech.stt.interval.count` –
   Allows some kind of continuous speech transcription beyond the limitations
   from above setting, by resuming recording immediately after an interval
   elapsed. With this setting you can configure, how many times recording is to
   be resumed automatically unless interrupted by user interaction.
   Defaults to 1, meaning no automatic resumption. 0 means unlimited
   resumption.

 * `kaleidok.examples.kaleidoscope.images.intial` –
   A space-separated list of URLs to use as initial images. Relative URLs are
   rooted in the `images` sub-directory of the applet path (inside `resources`
   in the development environment). Kaleidoscope displays 5 different images
   initially.

 * `kaleidok.examples.kaleidoscope.caches.[path|size]` –
   Path location and maximum size in bytes of Kaleidoscope's cache. Relative
   paths are interpreted as subdirectories of your platform's default cache
   directory.


[configuration]: #configuration
