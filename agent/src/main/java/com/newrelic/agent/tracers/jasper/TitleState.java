package com.newrelic.agent.tracers.jasper;

import java.util.regex.Matcher;

import com.newrelic.agent.Transaction;

public class TitleState extends AbstractRUMState {
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
            throws Exception {
        Matcher matcher = TITLE_END.matcher(text);

        if (matcher.find()) {
            String s = text.substring(0, matcher.end());
            writeText(tx, generator, node, s);
            s = text.substring(matcher.end());
            return PRE_META_STATE.process(tx, generator, node, s);
        }

        matcher = HEAD_END_PATTERN.matcher(text);
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