package com.getflourish.stt;


public class Response
{
  Result[] result;

  public class Result
  {
    Alternative[] alternative;
    int result_index;
    int status = 0;

    public class Alternative
    {
      String transcript;
      float confidence;
    }
  }
}
