package kaleidok.google.speech;


import com.google.gson.annotations.Expose;

import java.io.Serializable;


public class SttResponse implements Serializable
{
  private static final long serialVersionUID = 3089023760201884145L;

  @Expose
  public Result[] result;


  public boolean isEmpty()
  {
    return result == null || result.length == 0;
  }


  public Result.Alternative getTopAlternative()
  {
    return isEmpty() ? null : result[0].alternative[0];
  }


  public static class Result implements Serializable
  {
    private static final long serialVersionUID = -2839378636983233909L;

    @Expose
    public Alternative[] alternative;

    @Expose
    public int result_index;


    public static class Alternative implements Serializable
    {
      private static final long serialVersionUID = 7753634651444144012L;

      @Expose
      public String transcript;

      @Expose
      public float confidence = Float.NaN;
    }
  }
}
