package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuoteSate extends AbstractRUMState
{
  public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
    throws Exception
  {
    Matcher nextQuoteMatcher = QUOTE_PATTERN.matcher(text);

    if (nextQuoteMatcher.find()) {
      String s = text.substring(0, nextQuoteMatcher.end());
      writeText(tx, generator, node, s);
      s = text.substring(nextQuoteMatcher.end());
      return META_STATE.process(tx, generator, node, s);
    }

    Matcher matcher = HEAD_END_PATTERN.matcher(text);
    if (matcher.find()) {
      String s = text.substring(0, matcher.start());
      writeText(tx, generator, node, s);
      writeHeader(generator);
      s = text.substring(matcher.start());
      return BODY_STATE.process(tx, generator, node, s);
    }

    writeText(tx, generator, node, text);
    return this;
  }
}