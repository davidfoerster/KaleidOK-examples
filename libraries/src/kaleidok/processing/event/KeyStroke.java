package kaleidok.processing.event;

import com.jogamp.newt.event.KeyEvent;
import jogamp.newt.awt.event.AWTNewtEventFactory;
import kaleidok.util.Objects;
import processing.core.PApplet;
import processing.core.PConstants;

import static com.jogamp.newt.event.InputEvent.META_MASK;
import static com.jogamp.newt.event.KeyEvent.*;
import static kaleidok.processing.event.KeyEventSupport.dummySurfaceHolder;


public class KeyStroke
{
  @SuppressWarnings("unused")
  public static final short
    KEY_PRESSED = EVENT_KEY_PRESSED,
    KEY_RELEASED = EVENT_KEY_RELEASED;


  public static final KeyStroke
    fullscreenKeystroke = (PApplet.platform == PConstants.MACOSX) ?
      keySymbolStroke(KEY_PRESSED, META_MASK, VK_F) :
      keySymbolStroke(KEY_PRESSED, 0, VK_F11);


  public static KeyStroke keyCodeStroke( short eventType, int modifiers,
    short keyCode )
  {
    return new KeyStroke(eventType, modifiers, KeyType.CODE, keyCode);
  }


  public static KeyStroke keySymbolStroke( short eventType, int modifiers,
    short keySymbol )
  {
    return new KeyStroke(eventType, modifiers, KeyType.SYMBOL, keySymbol);
  }


  public static KeyStroke keyCharStroke( short eventType, int modifiers,
    char keyChar )
  {
    return new KeyStroke(eventType, modifiers, KeyType.CODE, keyChar);
  }


  public enum KeyType
  {
    CODE {
      @Override
      public short getKeyCode( int v ) { return (short) v; }

      @Override
      public int getKeyValue( KeyEvent ev ) { return ev.getKeyCode(); }
    },

    SYMBOL {
      @Override
      public short getKeySymbol( int v ) { return (short) v; }

      @Override
      public int getKeyValue( KeyEvent ev ) { return ev.getKeySymbol(); }
    },

    CHAR {
      @Override
      public char getKeyChar( int v ) { return (char) v; }

      @Override
      public int getKeyValue( KeyEvent ev ) { return ev.getKeyChar(); }
    };


    public short getKeyCode( int v ) { return VK_UNDEFINED; }

    public short getKeySymbol( int v ) { return VK_UNDEFINED; }

    public char getKeyChar( int v ) { return NULL_CHAR; }

    public abstract int getKeyValue( KeyEvent ev );
  }


  public final short eventType;

  public final int modifiers;

  public final KeyType keyType;

  public final int keyValue;


  protected KeyStroke( short eventType, int modifiers, KeyType keyType,
    int keyValue )
  {
    java.util.Objects.requireNonNull(keyType);
    this.eventType = eventType;
    this.modifiers = modifiers;
    this.keyType = keyType;
    this.keyValue = keyValue;
  }


  public short getKeyCode()
  {
    return keyType.getKeyCode(keyValue);
  }


  public short getKeySymbol()
  {
    return keyType.getKeySymbol(keyValue);
  }


  public char getKeyChar()
  {
    return keyType.getKeyChar(keyValue);
  }


  @Override
  public int hashCode()
  {
    return
      Objects.hashCode(eventType,
      Objects.hashCode(modifiers,
      Objects.hashCode(keyValue, keyType.hashCode())));
  }


  @Override
  public boolean equals( Object obj )
  {
    if (obj == this)
      return true;
    if (!(obj instanceof KeyStroke))
      return false;

    KeyStroke o = (KeyStroke) obj;
    return
      eventType == o.eventType &&
      modifiers == o.modifiers &&
      keyType == o.keyType &&
      keyValue == o.keyValue;
  }


  public boolean matches( KeyEvent ev )
  {
    return ev != null &&
      eventType == ev.getEventType() &&
      modifiers == ev.getModifiers() &&
      keyValue == keyType.getKeyValue(ev);
  }


  public boolean matches( java.awt.event.KeyEvent ev )
  {
    return matches(AWTNewtEventFactory.createKeyEvent(ev, dummySurfaceHolder));
  }


  public boolean matches( processing.event.KeyEvent ev )
  {
    if (ev == null)
      return false;
    Object nativeEvent = ev.getNative();
    if (nativeEvent instanceof KeyEvent)
      return matches((KeyEvent) nativeEvent);
    if (nativeEvent instanceof java.awt.event.KeyEvent)
      return matches((java.awt.event.KeyEvent) nativeEvent);
    throw new UnsupportedOperationException(
      nativeEvent.getClass().getCanonicalName() +
        " events are currently unsupported");
  }
}
