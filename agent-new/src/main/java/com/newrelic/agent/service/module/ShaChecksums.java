package com.newrelic.agent.service.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShaChecksums
{
  public static String computeSha(URL url)
    throws NoSuchAlgorithmException, IOException
  {
    MessageDigest md = MessageDigest.getInstance("SHA1");
    InputStream inputStream = null;
    try {
      inputStream = EmbeddedJars.getInputStream(url);

      DigestInputStream dis = new DigestInputStream(inputStream, md);
      byte[] buffer = new byte[8192];

      while (dis.read(buffer) != -1);
      byte[] mdbytes = md.digest();

      StringBuffer sb = new StringBuffer(40);
      for (int i = 0; i < mdbytes.length; i++) {
        sb.append(Integer.toString((mdbytes[i] & 0xFF) + 256, 16).substring(1));
      }

      return sb.toString();
    } finally {
      if (null != inputStream)
        inputStream.close();
    }
  }
}