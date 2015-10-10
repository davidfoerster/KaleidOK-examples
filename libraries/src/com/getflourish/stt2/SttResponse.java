package com.getflourish.stt2;


public class SttResponse
{
  public Result[] result;

  public boolean isEmpty()
  {
    return result == null || result.length == 0;
  }


  public Result.Alternative getTopAlternative()
  {
    return isEmpty() ? null : result[0].alternative[0];
  }


  public class Result
  {
    public Alternative[] alternative;
    public int result_index;

    public class Alternative
    {
      public String transcript;
      public float confidence = Float.NaN;
    }
  }
}
