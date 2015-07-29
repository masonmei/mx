package com.newrelic.agent.util;

import com.newrelic.deps.org.json.simple.JSONObject;
import com.newrelic.deps.org.json.simple.JSONStreamAware;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class JSONException extends Exception
  implements JSONStreamAware
{
  private static final long serialVersionUID = 3132223563667774992L;

  public JSONException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public JSONException(String message) {
    super(message);
  }

  public JSONException(Throwable cause) {
    super(cause);
  }

  public void writeJSONString(Writer out) throws IOException
  {
    JSONObject.writeJSONString(new HashMap()
    {
    }
    , out);
  }
}