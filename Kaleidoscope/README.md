# Kaleidoscope

**Note:** Image discovery and retrieval is defunct since [Chromatik], the web
service behind this function, went defunct itself.


## Setup

 1. [Request a Flickr API key](https://www.flickr.com/services/api/keys/apply/)
    and consult the [Configuration] section on where to put it for Kaleidoscope
    to find.

 2. Similarly [request a Google API access key](https://www.chromium.org/developers/how-tos/api-keys)
    and provide it to Kaleidoscope.

 3. Prepare a [configuration] for Kaleidoscope.

 4. Some libraries need to be set up, but I'm not going into that now.


## Usage

 1. Run KaleidoscopeApp as stand-alone Java application or use the included run
    IntelliJ configuration.

 2. Click the recorder button in the controls window or press <kbd>I</kbd> to 
    start recording from the default microphone line; click the recorder button
    again or press <kbd>O</kbd> to stop. The speech in the recorded section is 
    transcribed, the resulting text synesthetised and the synesthetiation 
    result used to search for images with Chromatik.

 3. If you're unwilling or unable to speak to Kaleidoscope through a microphone,
    use the two text fields below the recorder button in the controls window.
    The upper one is for a message to synesthetise, the lower one for search 
    terms for Chromatik's image search. You can run perform a synesthetiation 
    and image search by having the upper text field in focus and pressing 
    <kbd>ENTER</kbd>/<kbd>RETURN</kbd>.

 4. <kbd>F11</kbd> (Windows/Linux) or <kbd>⌘</kbd>+<kbd>F</kbd> (macOS) toggles 
    full screen display of Kaleidoscope.


## Configuration

The most convenient way to configure Kaleidoscope is through the integrated 
configuration editor:

 1. Click on the *Configuration window* tool button at the bottom of the 
    controls window.

 2. Double-click the cells in the right column of the configuration table to 
    edit their value. Most importantly, add the required API keys (see below).

Below is a description of some the most important options. All parameters
except API keys are optional and have sensible defaults.

 * *Flickr > access key* and *Transcription service > API access key*
  
    API access keys for Flickr and Google respectively. If the key is comprised 
    of multiple parts, separate them with a colon (`:`). Example:

        e0b92403f258c35c6b43d2e21c640f9f:bd7a0f0bcc5dfc25

    If the value to of these parameters start with `@`, the remainder is
    interpreted as the path to a resource file containing the key, where multiple
    parts are separated by newline characters.

        e0b92403f258c35c6b43d2e21c640f9f
        bd7a0f0bcc5dfc25

    The special parameter value `!MOCK` results in a mock implementation of the
    service connectors, that doesn't use the actual service, but returns a valid,
    pre-recorded result. For Google's speech-to-text service the mock mode can
    only be set through the key `com.google.developer.api.key` in the 
    properties file `KaleidoscopeApp.properties`.

    Note: The above example does not contain a working key, just a randomly
    generated look-alike.


[configuration]: #configuration
[Chromatik]: http://labs.exalead.com/project/chromatik
