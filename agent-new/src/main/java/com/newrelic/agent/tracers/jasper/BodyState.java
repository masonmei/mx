package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BodyState extends AbstractRUMState
{
  public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
    throws Exception
  {
    Matcher matcher = SCRIPT_PATTERN.matcher(text);
    if (matcher.find()) {
      String begin = text.substring(0, matcher.start());
      RUMState state = process(tx, generator, node, begin);
      String s = text.substring(matcher.start());
      if (state == DONE_STATE) {
        return DONE_STATE.process(tx, generator, node, s);
      }
      return SCRIPT_STATE.process(tx, generator, node, s);
    }

    matcher = BODY_END_PATTERN.matcher(text);
    if (matcher.find()) {
      String s = text.substring(0, matcher.start());
      writeText(tx, generator, node, s);
      writeFooter(generator);
      s = text.substring(matcher.start());
      return DONE_STATE.process(tx, generator, node, s);
    }
    matcher = HTML_END_PATTERN.matcher(text);
    if (matcher.find()) {
      String s = text.substring(0, matcher.start());
      writeText(tx, generator, node, s);
      writeFooter(generator);
      s = text.substring(matcher.start());
      return DONE_STATE.process(tx, generator, node, s);
    }

    writeText(tx, generator, node, text);
    return this;
  }
}