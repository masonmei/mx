package com.newrelic.api.agent;

import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * The Java agent's logging interface.
 * 
 * @author sdaubin
 * 
 */
public interface Logger {

    /**
     * Returns true if the given log level will be logged. Generally this method should NOT be used - just call the
     * {@link #log(Level, String, Object...)} methods with the message broken into parts. The overhead of the
     * concatenation will not be incurred if the log level isn't met.
     * 
     * @param level The level to be verified.
     * @return True if a message could be logged at the given level, else false.
     * @since 3.9.0
     */
    boolean isLoggable(Level level);

    /**
     * Concatenate the given parts and log them at the given level. If a part is <code>null</code>, its value will be
     * represented as "null". If a part is a <code>Class</code>, the value of {@link Class#getName()} will be used.
     * 
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param parts The parts to be placed in the log message using the {@link MessageFormat} style
     * @since 3.9.0
     */
    void log(Level level, String pattern, Object... parts);

    /**
     * Log a message with given Throwable information. Concatenate the given msg and log them at the given level. If a
     * msg is <code>null</code>, its value will be represented as "null". If a part is a
     * <code>Class</code>, the value of {@link Class#getName()} will be used.
     * 
     * @param level The level at which the message should be logged.
     * @param t The exception to be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param msg The parts to be placed in the log message using the {@link MessageFormat} style.
     * @since 3.9.0
     * 
     */
    void log(Level level, Throwable t, String pattern, Object... msg);

    /**
     * Concatenate the given parts and log them at the given level. If a part is <code>null</code>, its value will be
     * represented as "null". If a part is a <code>Class</code>, the value of {@link Class#getName()} will be used.
     * 
     * @param childName The name of the child logger.
     * @param level The level at which the message should be logged.
     * @param pattern A message format pattern in the {@link MessageFormat} style.
     * @param parts The parts to be placed in the log message using the {@link MessageFormat} style.
     * @since 3.9.0
     * 
     */
    void logToChild(String childName, Level level, String pattern, Object... parts);
}
