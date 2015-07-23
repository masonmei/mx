package com.newrelic.agent.tracers.jasper;

import java.util.regex.Matcher;

import com.newrelic.agent.Transaction;

public class CommentState extends AbstractRUMState {
    public RUMState process(Transaction tx, GenerateVisitor generator, TemplateText node, String text)
            throws Exception {
        Matcher commentMatcher = END_COMMENT.matcher(text);

        if (commentMatcher.find()) {
            String s = text.substring(0, commentMatcher.start());
            writeText(tx, generator, node, s);
            s = text.substring(commentMatcher.start());
            return PRE_META_STATE.process(tx, generator, node, s);
        }

        writeText(tx, generator, node, text);
        return this;
    }
}