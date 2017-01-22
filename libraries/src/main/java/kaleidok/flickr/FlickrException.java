package kaleidok.flickr;

public class FlickrException extends Exception
{
  private static final long serialVersionUID = -4119100918511688709L;

  private int errorCode;

  private String pertainingObject;


  public FlickrException( String message, int errorCode, String pertainingObject )
  {
    super(message);
    this.errorCode = errorCode;
    this.pertainingObject = pertainingObject;
  }


  public FlickrException( String message, int errorCode, String pertainingObject, Throwable cause )
  {
    super(message, cause);
    this.errorCode = errorCode;
    this.pertainingObject = pertainingObject;
  }


  public int getErrorCode()
  {
    return errorCode;
  }

  public String getPertainingObject()
  {
    return pertainingObject;
  }


  public void setErrorCode( int errorCode )
  {
    this.errorCode = errorCode;
    cachedMessage = null;
  }

  public void setPertainingObject( String pertainingObject )
  {
    if (this.pertainingObject != null)
      throw new IllegalStateException("Can’t change the pertaining object");

    if (pertainingObject != null)
    {
      this.pertainingObject = pertainingObject;
      cachedMessage = null;
    }
  }


  private String cachedMessage = null;

  @Override
  public String getMessage()
  {
    if (cachedMessage == null) {
      cachedMessage = String.format("%s (%d): %s",
        super.getMessage(), errorCode, pertainingObject);
    }
    return cachedMessage;
  }
}
