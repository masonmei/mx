package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreMetaState extends AbstractRUMState
{
  public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
    throws Exception
  {
    Matcher tagMatcher = START_TAG_PATTERN.matcher(text);

    if (tagMatcher.find()) {
      String begin = text.substring(0, tagMatcher.start());
      writeText(tx, generator, node, begin);
      String s = text.substring(tagMatcher.start());

      if (s.startsWith("</head>"))
      {
        writeHeader(generator);
        return BODY_STATE.process(tx, generator, node, s);
      }if ((s.startsWith("<meta ")) || (s.startsWith("<META")))
      {
        return META_STATE.process(tx, generator, node, s);
      }if (s.startsWith("<title>"))
        return TITLE_STATE.process(tx, generator, node, s);
      if (s.startsWith("<!--")) {
        return COMMENT_STATE.process(tx, generator, node, s);
      }

      writeHeader(generator);
      return BODY_STATE.process(tx, generator, node, s);
    }

    writeText(tx, generator, node, text);
    return this;
  }
}