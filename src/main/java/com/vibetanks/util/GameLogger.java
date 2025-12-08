package com.vibetanks.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple configurable logging utility for Vibe-Tanks.
 * Provides log levels and can be easily replaced with SLF4J/Logback later.
 *
 * Usage:
 *   private static final GameLogger LOG = GameLogger.getLogger(MyClass.class);
 *   LOG.info("Player {} joined the game", playerName);
 *   LOG.debug("Position: ({}, {})", x, y);
 */
public class GameLogger {

    public enum Level {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4),
        OFF(5);

        private final int priority;

        Level(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    private static Level globalLevel = Level.INFO;
    private static boolean showTimestamp = true;
    private static boolean showClassName = true;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final String className;

    private GameLogger(String className) {
        this.className = className;
    }

    /**
     * Get a logger for the specified class.
     */
    public static GameLogger getLogger(Class<?> clazz) {
        return new GameLogger(clazz.getSimpleName());
    }

    /**
     * Get a logger with a custom name.
     */
    public static GameLogger getLogger(String name) {
        return new GameLogger(name);
    }

    /**
     * Set the global log level. Messages below this level will not be printed.
     */
    public static void setLevel(Level level) {
        globalLevel = level;
    }

    /**
     * Get the current global log level.
     */
    public static Level getLevel() {
        return globalLevel;
    }

    /**
     * Enable or disable timestamp in log messages.
     */
    public static void setShowTimestamp(boolean show) {
        showTimestamp = show;
    }

    /**
     * Enable or disable class name in log messages.
     */
    public static void setShowClassName(boolean show) {
        showClassName = show;
    }

    /**
     * Configure logging for server mode (more verbose, no timestamp needed for console).
     */
    public static void configureForServer() {
        setLevel(Level.DEBUG);
        setShowTimestamp(true);
        setShowClassName(true);
    }

    /**
     * Configure logging for client mode (less verbose).
     */
    public static void configureForClient() {
        setLevel(Level.INFO);
        setShowTimestamp(false);
        setShowClassName(false);
    }

    /**
     * Configure logging for debug mode (all messages).
     */
    public static void configureForDebug() {
        setLevel(Level.TRACE);
        setShowTimestamp(true);
        setShowClassName(true);
    }

    // Logging methods

    public void trace(String message) {
        log(Level.TRACE, message);
    }

    public void trace(String format, Object... args) {
        log(Level.TRACE, format, args);
    }

    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    public void debug(String format, Object... args) {
        log(Level.DEBUG, format, args);
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void info(String format, Object... args) {
        log(Level.INFO, format, args);
    }

    public void warn(String message) {
        log(Level.WARN, message);
    }

    public void warn(String format, Object... args) {
        log(Level.WARN, format, args);
    }

    public void error(String message) {
        log(Level.ERROR, message);
    }

    public void error(String format, Object... args) {
        log(Level.ERROR, format, args);
    }

    public void error(String message, Throwable t) {
        log(Level.ERROR, message);
        if (t != null && globalLevel.getPriority() <= Level.ERROR.getPriority()) {
            t.printStackTrace(System.err);
        }
    }

    // Check if level is enabled (useful for expensive string operations)

    public boolean isTraceEnabled() {
        return globalLevel.getPriority() <= Level.TRACE.getPriority();
    }

    public boolean isDebugEnabled() {
        return globalLevel.getPriority() <= Level.DEBUG.getPriority();
    }

    public boolean isInfoEnabled() {
        return globalLevel.getPriority() <= Level.INFO.getPriority();
    }

    // Internal logging

    private void log(Level level, String message) {
        if (level.getPriority() < globalLevel.getPriority()) {
            return;
        }

        StringBuilder sb = new StringBuilder();

        if (showTimestamp) {
            sb.append("[").append(TIME_FORMAT.format(LocalDateTime.now())).append("] ");
        }

        sb.append("[").append(level.name()).append("]");

        if (showClassName) {
            sb.append(" [").append(className).append("]");
        }

        sb.append(" ").append(message);

        if (level == Level.ERROR || level == Level.WARN) {
            System.err.println(sb);
        } else {
            System.out.println(sb);
        }
    }

    private void log(Level level, String format, Object... args) {
        if (level.getPriority() < globalLevel.getPriority()) {
            return;
        }

        String message = formatMessage(format, args);
        log(level, message);
    }

    /**
     * Format message with {} placeholders (SLF4J style).
     */
    private String formatMessage(String format, Object... args) {
        if (args == null || args.length == 0) {
            return format;
        }

        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int i = 0;

        while (i < format.length()) {
            if (i < format.length() - 1 && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    result.append(args[argIndex++]);
                } else {
                    result.append("{}");
                }
                i += 2;
            } else {
                result.append(format.charAt(i));
                i++;
            }
        }

        return result.toString();
    }
}
