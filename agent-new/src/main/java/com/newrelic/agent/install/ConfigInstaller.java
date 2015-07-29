package com.newrelic.agent.install;

import com.newrelic.agent.Agent;
import com.newrelic.agent.util.Streams;
import com.newrelic.agent.util.Strings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;

public class ConfigInstaller
{
  private static final String REPLACE_WITH_YOUR_LICENSE_KEY = "replace_with_your_license_key";
  private static final String LICENSE_KEY_CONFIG_PARAM = "<%= license_key %>";
  private static final String GENERATE_FOR_USER_CONFIG_PARAM = "<%= generated_for_user %>";

  public static boolean isConfigInstalled(File configDir)
  {
    if ((configDir != null) && (configDir.exists()) && (configDir.isDirectory())) {
      return new File(configDir, "newrelic.yml").exists();
    }

    return false;
  }

  public static String configPath(File configDir) {
    return new File(configDir, "newrelic.yml").getAbsolutePath();
  }

  public static void install(String licenseKey, File configDir) throws Exception {
    generateConfig(licenseKey == null ? "replace_with_your_license_key" : licenseKey, configDir);
  }

  private static void generateConfig(String licenseKey, File configDir) throws Exception {
    InputStream inStream = ConfigInstaller.class.getClassLoader().getResourceAsStream("newrelic.yml");
    if (inStream != null) {
      String generatedFrom = getGeneratedFromString();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      try {
        Streams.copy(inStream, output);

        String yaml = output.toString().replace("<%= generated_for_user %>", generatedFrom).replace("<%= license_key %>", licenseKey);

        FileOutputStream fileOut = new FileOutputStream(configPath(configDir));
        try {
          Streams.copy(new ByteArrayInputStream(yaml.getBytes()), fileOut, yaml.getBytes().length);
        } finally {
          fileOut.close();
        }
      } finally {
        inStream.close();
      }
    } else {
      throw new IOException("Unable to open newrelic.yml template");
    }
  }

  private static String getGeneratedFromString() {
    return MessageFormat.format("Generated on {0}, from version {1}", new Object[] { new Date(), Agent.getVersion() });
  }

  public static boolean isLicenseKeyEmpty(String license) {
    return (license == null) || (license.equals("replace_with_your_license_key")) || (license.equals("<%= license_key %>")) || (Strings.isEmpty(license)) || (Strings.isEmpty(license.trim()));
  }
}