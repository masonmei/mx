package com.newrelic.assembly;

import com.newrelic.deps.org.reflections.Reflections;
import com.newrelic.deps.org.reflections.serializers.JsonSerializer;
import com.newrelic.deps.org.reflections.util.ConfigurationBuilder;

public class BuildManifest
{
  public static void main(String[] args)
  {
    String buildDir = args[0];
    Reflections reflections = new Reflections(new ConfigurationBuilder().forPackages(new String[] { "com.newrelic" }).setSerializer(new JsonSerializer()));

    reflections.save(buildDir + "/newrelic-manifest.json");
  }
}