package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetaState extends AbstractRUMState
{
  public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
    throws Exception
  {
    Matcher tagMatcher = END_TAG_OR_QUOTE_PATTERN.matcher(text);
    if (tagMatcher.find())
    {
      if (tagMatcher.group().equals("\""))
      {
        String s = text.substring(0, tagMatcher.end());
        writeText(tx, generator, node, s);
        s = text.substring(tagMatcher.end());
        return QUOTE_STATE.process(tx, generator, node, s);
      }if (tagMatcher.group().equals("'"))
      {
        String s = text.substring(0, tagMatcher.end());
        writeText(tx, generator, node, s);
        s = text.substring(tagMatcher.end());
        return SINGLE_QUOTE_STATE.process(tx, generator, node, s);
      }

      String s = text.substring(0, tagMatcher.start());
      writeText(tx, generator, node, s);
      s = text.substring(tagMatcher.start());
      return PRE_META_STATE.process(tx, generator, node, s);
    }

    writeText(tx, generator, node, text);
    return this;
  }
}