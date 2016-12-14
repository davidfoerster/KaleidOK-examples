package kaleidok.image.filter;

import kaleidok.util.function.BinaryFloatFunction;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Objects;

import static kaleidok.util.Math.clamp;


public class HSBAdjustFilter extends HSBImageFilter
{
  public enum FilterMode implements BinaryFloatFunction
  {
    ADD('+')
      {
        @Override
        public float applyAsFloat( float a, float b )
        {
          return a + b;
        }
      },
    
    SUBTRACT('-')
      {
        @Override
        public float applyAsFloat( float a, float b )
        {
          return a - b;
        }
      },
    
    MULTIPLY('*')
      {
        @Override
        public float applyAsFloat( float a, float b )
        {
          return a * b;
        }
      },
    
    DIVIDE('/')
      {
        @Override
        public float applyAsFloat( float a, float b )
        {
          return a / b;
        }
      },

    POWER('^')
      {
        @Override
        public float applyAsFloat( float a, float b )
        {
          return (float) Math.pow(a, b);
        }
      };


    public final char symbol;


    FilterMode( char symbol )
    {
      this.symbol = symbol;
    }


    public static FilterMode fromSymbol( char symbol )
      throws IllegalArgumentException
    {
      switch (symbol)
      {
      case '+':
        return ADD;

      case '-':
        return SUBTRACT;

      case '*':
        return MULTIPLY;

      case '/':
        return DIVIDE;

      case '^':
        return POWER;

      default:
        throw new IllegalArgumentException(
          "Unknown filter mode symbol: \"" +
            StringEscapeUtils.escapeJava(Character.toString(symbol)) + '\"');
      }
    }
  }
  
  
  public float hue, saturation, brightness;

  public BinaryFloatFunction filterMode;


  public HSBAdjustFilter( float hue, float saturation, float brightness,
    FilterMode filterMode )
  {
    this(hue, saturation, brightness, (BinaryFloatFunction) filterMode);
  }


  public HSBAdjustFilter( float hue, float saturation, float brightness,
    BinaryFloatFunction filterMode )
  {
    this.hue = hue;
    this.saturation = saturation;
    this.brightness = brightness;
    this.filterMode = Objects.requireNonNull(filterMode);
    canFilterIndexColorModel = true;
  }


  public HSBAdjustFilter( float hue, float saturation, float brightness )
  {
    this(hue, saturation, brightness, FilterMode.ADD);
  }


  public HSBAdjustFilter()
  {
    this(0, 0, 0);
  }


  @Override
  public boolean isNeutral()
  {
    if (filterMode instanceof FilterMode)
    {
      switch ((FilterMode) filterMode)
      {
      case ADD:
      case SUBTRACT:
        return saturation == 0 && brightness == 0 && normalizeHue(hue) == 0;

      case MULTIPLY:
      case DIVIDE:
      case POWER:
        return saturation == 1 && brightness == 1 && hue == 1;
      }
    }
    return false;
  }


  @Override
  public float[] filterHSB( int x, int y, float[] hsb )
  {
    BinaryFloatFunction filterMode = this.filterMode;
    hsb[0] = normalizeHue(filterMode.applyAsFloat(hsb[0], hue));
    hsb[1] = clamp(filterMode.applyAsFloat(hsb[1], saturation), 0, 1);
    hsb[2] = clamp(filterMode.applyAsFloat(hsb[2], brightness), 0, 1);
    return hsb;
  }


  private static float normalizeHue( float hue )
  {
    return hue % (float)(Math.PI * 2);
  }


  @Override
  public String toString()
  {
    return getClass().getName() + '[' + filterMode + ' ' + '(' +
      hue + ',' + ' ' + saturation + ',' + ' ' + brightness + ')' + ']';
  }
}
