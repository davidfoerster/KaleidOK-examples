package kaleidok.chromatik;

import kaleidok.chromatik.data.ChromatikResponse;
import kaleidok.concurrent.Callback;

import java.applet.Applet;
import java.io.IOException;


public class CallbackChromasthetiator extends Chromasthetiator
{
  public Callback<ChromatikResponse> searchResultHandler = null;

  public CallbackChromasthetiator( Applet parent, Callback<ChromatikResponse> srh )
    throws IOException
  {
    super(parent);
    this.searchResultHandler = srh;
  }

  public void issueQuery( String text )
  {
    ChromatikResponse response;
    try {
      response = query(text);
    } catch (IOException ex) {
      ex.printStackTrace();
      return;
    }

    Callback<ChromatikResponse> searchResultHandler = this.searchResultHandler;
    if (searchResultHandler != null)
      searchResultHandler.call(response);
  }
}
