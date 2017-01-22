package kaleidok.javafx.beans.property.adapter.preference;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyProperty;
import kaleidok.util.Reflection;
import kaleidok.util.Strings;
import kaleidok.util.logging.LoggingUtils;
import kaleidok.util.prefs.PreferenceUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.translate.*;

import javax.annotation.Nonnull;
import java.text.Normalizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static kaleidok.util.Objects.requireNonNull;
import static kaleidok.util.logging.LoggingUtils.logThrown;
import static kaleidok.util.prefs.PreferenceUtils.KEY_PART_DELIMITER;


public abstract class ReadOnlyPropertyPreferencesAdapter<T, P extends ReadOnlyProperty<? extends T>>
{
  public final P property;

  public final Preferences preferences;

  public final String key;

  @SuppressWarnings("NonConstantLogger")
  protected final Logger logger;

  protected static final Level LOG_LEVEL = Level.FINEST;


  protected ReadOnlyPropertyPreferencesAdapter( @Nonnull P property )
  {
    this (property, null, true, null, true, null, true);
  }

  protected ReadOnlyPropertyPreferencesAdapter( @Nonnull P property,
    @Nonnull Class<?> beanClass )
  {
    this(property, beanClass, false, null, true, null, true);
  }

  protected ReadOnlyPropertyPreferencesAdapter( @Nonnull P property,
    @Nonnull Preferences preferences )
  {
    this(property, null, true, preferences, false, null, true);
  }

  protected ReadOnlyPropertyPreferencesAdapter( @Nonnull P property,
    @Nonnull String prefix )
  {
    this(property, null, true, null, true, prefix, false);
  }

  protected ReadOnlyPropertyPreferencesAdapter( @Nonnull P property,
    @Nonnull Preferences preferences, @Nonnull String prefix )
  {
    this(property, null, true, preferences, false, prefix, false);
  }


  private ReadOnlyPropertyPreferencesAdapter( @Nonnull P property,
    Class<?> beanClass, boolean useDefaultBeanClass,
    Preferences preferences, boolean useDefaultPreferences,
    String prefix, boolean useDefaultPrefix )
  {
    this.property = requireNonNull(property, "property");
    beanClass = getBeanClass(property, beanClass, useDefaultBeanClass,
      prefix, useDefaultPrefix);
    this.key = getPreferenceKey(property.getName(),
      getPrefixFromDefault(beanClass, prefix, useDefaultPrefix));
    this.preferences =
      (requireNonNull(preferences, useDefaultPreferences, "preferences") == null) ?
        Preferences.userNodeForPackage(beanClass) :
        preferences;

    logger = LoggingUtils.getLogger(beanClass);
  }


  @Nonnull
  private static Class<?> getBeanClass( @Nonnull ReadOnlyProperty<?> property,
    Class<?> beanClass, boolean useDefaultBeanClass,
    String prefix, boolean useDefaultPrefix )
  {
    if (requireNonNull(beanClass, useDefaultBeanClass, "bean class") == null)
      beanClass = requireNonNull(property.getBean(), "bean").getClass();

    // Check for array bean
    if (useDefaultPrefix && prefix == null && beanClass.isArray())
      throw new IllegalArgumentException("arrays aren’t beans");

    return beanClass;
  }


  public static final Pattern NON_WORD_SEQUENCE =
    Pattern.compile("[\\W._-]{2,}|[^\\w._-]+",
      Pattern.UNICODE_CHARACTER_CLASS);


  @Nonnull
  private static String getPrefixFromDefault( Class<?> beanClass,
    String prefix, boolean useDefaultPrefix )
  {
    if (requireNonNull(prefix, useDefaultPrefix, "prefix") == null)
      prefix = Reflection.getAnonymousClassSimpleName(beanClass);

    assert prefix.isEmpty() || !NON_WORD_SEQUENCE.matcher(prefix).find() :
      "Weird preference key prefix encountered: " + StringEscapeUtils.escapeJava(prefix);

    return prefix;
  }


  @Nonnull
  private static String getPreferenceKey( @Nonnull String name,
    @Nonnull String prefix )
  {
    String key =
      Strings.join(prefix, scrubPropertyName(name), KEY_PART_DELIMITER)
        .toString();
    if (key.isEmpty())
      throw new IllegalArgumentException("empty prefix and property name");
    assert Normalizer.isNormalized(key, Normalizer.Form.NFC);
    return key;
  }


  private static CharSequence scrubPropertyName( String name )
  {
    return name.isEmpty() ?
      name :
      Strings.replaceAll(NON_WORD_SEQUENCE, name,
        (matcher, input, sb) -> {
          int start = matcher.start();
          if (start == 0 || matcher.end() == input.length())
            return "";
          char c = input.charAt(start);
          return (c == KEY_PART_DELIMITER || c == '_') ? c : '-';
        });
  }


  private String stringRepresentation = null;

  @Override
  public String toString()
  {
    if (stringRepresentation == null)
    {
      String className = getClass().getName();
      stringRepresentation = new StringBuilder()
        .append(className,
          Math.max(className.lastIndexOf(KEY_PART_DELIMITER) + 1, 0),
          className.length())
        .append('[').append(preferences.absolutePath()).append('/').append(key)
        .append(" <=> ").append(property).append(']')
        .toString();
    }
    return stringRepresentation;
  }


  public void save()
  {
    if (logger != null && logger.isLoggable(LOG_LEVEL))
    {
      Object value = property.getValue();
      logger.log(LOG_LEVEL,
        (value != null) ?
          "Saving preference value {0}/{1} = \"{2}\"" :
          "Saving preference value {0}/{1} = null",
        new Object[]{ preferences.absolutePath(), key, value });
    }

    try
    {
      doSave();
    }
    catch (RuntimeException ex)
    {
      if (logger != null)
      {
        logThrown(logger, Level.WARNING, "Couldn’t save preference {0}/{1}", ex,
          new Object[]{ preferences.absolutePath(), key });
      }
    }
  }


  protected abstract void doSave();


  protected static final CharSequenceTranslator ESCAPE;
  static
  {
    String[][]
      javaCtrlCharsEscape = EntityArrays.JAVA_CTRL_CHARS_ESCAPE(),
      ourCtrlCharsEscape = new String[javaCtrlCharsEscape.length + 2][];
    ourCtrlCharsEscape[0] = new String[]{ "\\", "\\\\" };
    ourCtrlCharsEscape[1] = new String[]{ "\0", "\\0" };
    System.arraycopy(javaCtrlCharsEscape, 0, ourCtrlCharsEscape, 2,
      javaCtrlCharsEscape.length);

    ESCAPE = new AggregateTranslator(
      new LookupTranslator(ourCtrlCharsEscape),
      JavaUnicodeEscaper.outsideOf(32, Character.MAX_CODE_POINT),
      JavaUnicodeEscaper.between(
        Character.MIN_SURROGATE, Character.MAX_SURROGATE),
      JavaUnicodeEscaper.between(0xfffe, 0xffff));
  }

  protected static final CharSequenceTranslator UNESCAPE =
    StringEscapeUtils.UNESCAPE_JAVA;


  protected void put( String value )
  {
    if (value != null)
    {
      preferences.put(key, ESCAPE.translate(value));
    }
    else
    {
      preferences.remove(key);
    }
  }


  public boolean loadIfWritable()
  {
    return false;
  }


  private volatile InvalidationListener autoSaveListener = null;

  public boolean isAutoSave()
  {
    return autoSaveListener != null;
  }

  public synchronized void setAutoSave( boolean autoSave )
  {
    if (autoSave == isAutoSave())
      return;

    if (autoSave)
    {
      autoSaveListener = (observable) -> save();
      save();
      property.addListener(autoSaveListener);
    }
    else
    {
      property.removeListener(autoSaveListener);
      autoSaveListener = null;
    }
  }


  public static void saveAndFlush(
    Stream<? extends ReadOnlyPropertyPreferencesAdapter<?,?>> prefAdapters )
  {
    prefAdapters
      .peek(ReadOnlyPropertyPreferencesAdapter::save)
      .map((pa) -> pa.preferences)
      .distinct()
      .forEach(PreferenceUtils::flush);
  }
}
