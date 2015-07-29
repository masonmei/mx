package com.newrelic.agent.service.analytics;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;

public class CustomInsightsEvent extends AnalyticsEvent
{
  public CustomInsightsEvent(String type, long timestamp, Map<String, Object> attributes)
  {
    super(type, timestamp);
    this.userAttributes = attributes;
  }

  public void writeJSONString(Writer out)
    throws IOException
  {
    JSONObject intrinsics = new JSONObject();
    intrinsics.put("type", this.type);
    intrinsics.put("timestamp", Long.valueOf(this.timestamp));

    JSONArray.writeJSONString(Arrays.asList(new Map[] { intrinsics, this.userAttributes }), out);
  }
}