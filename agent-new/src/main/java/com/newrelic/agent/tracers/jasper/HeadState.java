package com.newrelic.agent.tracers.jasper;

import com.newrelic.agent.Transaction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeadState extends AbstractRUMState
{
  public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
    throws Exception
  {
    Matcher scriptMatcher = SCRIPT_PATTERN.matcher(text);
    Integer scriptIndex = null;
    if (scriptMatcher.find()) {
      scriptIndex = Integer.valueOf(scriptMatcher.end());
    }

    Matcher matcher = HEAD_PATTERN.matcher(text);
    if (matcher.find()) {
      if (isScriptFirst(scriptIndex, Integer.valueOf(matcher.end()))) {
        return PRE_HEAD_SCRIPT_STATE.process(tx, generator, node, text);
      }
      String s = text.substring(0, matcher.end());
      writeText(tx, generator, node, s);
      s = text.substring(matcher.end());
      return PRE_META_STATE.process(tx, generator, node, s);
    }

    matcher = HEAD_END_PATTERN.matcher(text);
    if (matcher.find()) {
      if (isScriptFirst(scriptIndex, Integer.valueOf(matcher.end())))
      {
        String s = text.substring(0, scriptMatcher.start());
        writeText(tx, generator, node, s);
        writeHeader(generator);
        s = text.substring(scriptMatcher.start());
        return SCRIPT_STATE.process(tx, generator, node, s);
      }
      String s = text.substring(0, matcher.start());
      writeText(tx, generator, node, s);
      writeHeader(generator);
      s = text.substring(matcher.start());
      return BODY_STATE.process(tx, generator, node, s);
    }
    matcher = BODY_END_PATTERN.matcher(text);
    if (matcher.find()) {
      if (isScriptFirst(scriptIndex, Integer.valueOf(matcher.end())))
      {
        return SCRIPT_STATE.process(tx, generator, node, text);
      }
      return BODY_STATE.process(tx, generator, node, text);
    }

    matcher = BODY_START_PATTERN.matcher(text);
    if (scriptIndex != null) {
      if (matcher.find()) {
        if (isScriptFirst(scriptIndex, Integer.valueOf(matcher.end()))) {
          return PRE_HEAD_SCRIPT_STATE.process(tx, generator, node, text);
        }
        return SCRIPT_STATE.process(tx, generator, node, text);
      }

      return PRE_HEAD_SCRIPT_STATE.process(tx, generator, node, text);
    }

    writeText(tx, generator, node, text);
    return this;
  }

  private boolean isScriptFirst(Integer scriptIndex, Integer headIndex) {
    if (scriptIndex == null)
      return false;
    if (headIndex == null) {
      return true;
    }
    return headIndex.intValue() > scriptIndex.intValue();
  }
}