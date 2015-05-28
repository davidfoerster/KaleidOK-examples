package com.getflourish.stt2;


public class Response
{
  public Result[] result;

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
