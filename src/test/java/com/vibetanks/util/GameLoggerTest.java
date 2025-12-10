package com.vibetanks.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameLogger Tests")
class GameLoggerTest {

    private GameLogger.Level originalLevel;
    private boolean originalShowTimestamp;
    private boolean originalShowClassName;

    @BeforeEach
    void setUp() {
        // Save original settings
        originalLevel = GameLogger.getLevel();
        // Reset to known state
        GameLogger.setLevel(GameLogger.Level.INFO);
        GameLogger.setShowTimestamp(false);
        GameLogger.setShowClassName(true);
    }

    @AfterEach
    void tearDown() {
        // Restore original settings
        GameLogger.setLevel(originalLevel);
    }

    @Nested
    @DisplayName("Logger Creation Tests")
    class LoggerCreationTests {

        @Test
        @DisplayName("getLogger with class should return logger")
        void getLoggerWithClassShouldReturnLogger() {
            GameLogger logger = GameLogger.getLogger(GameLoggerTest.class);
            assertNotNull(logger);
        }

        @Test
        @DisplayName("getLogger with string should return logger")
        void getLoggerWithStringShouldReturnLogger() {
            GameLogger logger = GameLogger.getLogger("TestLogger");
            assertNotNull(logger);
        }
    }

    @Nested
    @DisplayName("Log Level Tests")
    class LogLevelTests {

        @Test
        @DisplayName("setLevel should change global level")
        void setLevelShouldChangeGlobalLevel() {
            GameLogger.setLevel(GameLogger.Level.DEBUG);
            assertEquals(GameLogger.Level.DEBUG, GameLogger.getLevel());

            GameLogger.setLevel(GameLogger.Level.ERROR);
            assertEquals(GameLogger.Level.ERROR, GameLogger.getLevel());
        }

        @Test
        @DisplayName("Level enum should have correct priorities")
        void levelEnumShouldHaveCorrectPriorities() {
            assertTrue(GameLogger.Level.TRACE.getPriority() < GameLogger.Level.DEBUG.getPriority());
            assertTrue(GameLogger.Level.DEBUG.getPriority() < GameLogger.Level.INFO.getPriority());
            assertTrue(GameLogger.Level.INFO.getPriority() < GameLogger.Level.WARN.getPriority());
            assertTrue(GameLogger.Level.WARN.getPriority() < GameLogger.Level.ERROR.getPriority());
            assertTrue(GameLogger.Level.ERROR.getPriority() < GameLogger.Level.OFF.getPriority());
        }

        @Test
        @DisplayName("All log levels should be available")
        void allLogLevelsShouldBeAvailable() {
            GameLogger.Level[] levels = GameLogger.Level.values();
            assertEquals(6, levels.length);
        }
    }

    @Nested
    @DisplayName("Level Check Tests")
    class LevelCheckTests {

        @Test
        @DisplayName("isTraceEnabled should return true when level is TRACE")
        void isTraceEnabledShouldReturnTrueWhenLevelIsTrace() {
            GameLogger logger = GameLogger.getLogger("Test");
            GameLogger.setLevel(GameLogger.Level.TRACE);
            assertTrue(logger.isTraceEnabled());
        }

        @Test
        @DisplayName("isTraceEnabled should return false when level is INFO")
        void isTraceEnabledShouldReturnFalseWhenLevelIsInfo() {
            GameLogger logger = GameLogger.getLogger("Test");
            GameLogger.setLevel(GameLogger.Level.INFO);
            assertFalse(logger.isTraceEnabled());
        }

        @Test
        @DisplayName("isDebugEnabled should return true when level is DEBUG")
        void isDebugEnabledShouldReturnTrueWhenLevelIsDebug() {
            GameLogger logger = GameLogger.getLogger("Test");
            GameLogger.setLevel(GameLogger.Level.DEBUG);
            assertTrue(logger.isDebugEnabled());
        }

        @Test
        @DisplayName("isInfoEnabled should return true when level is INFO")
        void isInfoEnabledShouldReturnTrueWhenLevelIsInfo() {
            GameLogger logger = GameLogger.getLogger("Test");
            GameLogger.setLevel(GameLogger.Level.INFO);
            assertTrue(logger.isInfoEnabled());
        }

        @Test
        @DisplayName("isInfoEnabled should return false when level is WARN")
        void isInfoEnabledShouldReturnFalseWhenLevelIsWarn() {
            GameLogger logger = GameLogger.getLogger("Test");
            GameLogger.setLevel(GameLogger.Level.WARN);
            assertFalse(logger.isInfoEnabled());
        }
    }

    @Nested
    @DisplayName("Configuration Mode Tests")
    class ConfigurationModeTests {

        @Test
        @DisplayName("configureForServer should set DEBUG level")
        void configureForServerShouldSetDebugLevel() {
            GameLogger.configureForServer();
            assertEquals(GameLogger.Level.DEBUG, GameLogger.getLevel());
        }

        @Test
        @DisplayName("configureForClient should set INFO level")
        void configureForClientShouldSetInfoLevel() {
            GameLogger.configureForClient();
            assertEquals(GameLogger.Level.INFO, GameLogger.getLevel());
        }

        @Test
        @DisplayName("configureForDebug should set TRACE level")
        void configureForDebugShouldSetTraceLevel() {
            GameLogger.configureForDebug();
            assertEquals(GameLogger.Level.TRACE, GameLogger.getLevel());
        }
    }

    @Nested
    @DisplayName("Logging Method Tests")
    class LoggingMethodTests {

        private GameLogger logger;
        private ByteArrayOutputStream outputStream;
        private PrintStream originalOut;

        @BeforeEach
        void setUp() {
            logger = GameLogger.getLogger("Test");
            outputStream = new ByteArrayOutputStream();
            originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            GameLogger.setLevel(GameLogger.Level.TRACE);
            GameLogger.setShowTimestamp(false);
        }

        @AfterEach
        void tearDown() {
            System.setOut(originalOut);
        }

        @Test
        @DisplayName("info should output message at INFO level")
        void infoShouldOutputMessageAtInfoLevel() {
            logger.info("Test message");
            String output = outputStream.toString();
            assertTrue(output.contains("[INFO]"));
            assertTrue(output.contains("Test message"));
        }

        @Test
        @DisplayName("debug should output message at DEBUG level")
        void debugShouldOutputMessageAtDebugLevel() {
            logger.debug("Debug message");
            String output = outputStream.toString();
            assertTrue(output.contains("[DEBUG]"));
            assertTrue(output.contains("Debug message"));
        }

        @Test
        @DisplayName("trace should output message at TRACE level")
        void traceShouldOutputMessageAtTraceLevel() {
            logger.trace("Trace message");
            String output = outputStream.toString();
            assertTrue(output.contains("[TRACE]"));
            assertTrue(output.contains("Trace message"));
        }

        @Test
        @DisplayName("info with arguments should format message")
        void infoWithArgumentsShouldFormatMessage() {
            logger.info("Player {} scored {} points", "Alice", 100);
            String output = outputStream.toString();
            assertTrue(output.contains("Player Alice scored 100 points"));
        }

        @Test
        @DisplayName("debug with arguments should format message")
        void debugWithArgumentsShouldFormatMessage() {
            logger.debug("Position: ({}, {})", 10.5, 20.5);
            String output = outputStream.toString();
            assertTrue(output.contains("Position: (10.5, 20.5)"));
        }
    }

    @Nested
    @DisplayName("Error and Warn Output Tests")
    class ErrorAndWarnOutputTests {

        private GameLogger logger;
        private ByteArrayOutputStream errorStream;
        private PrintStream originalErr;

        @BeforeEach
        void setUp() {
            logger = GameLogger.getLogger("Test");
            errorStream = new ByteArrayOutputStream();
            originalErr = System.err;
            System.setErr(new PrintStream(errorStream));
            GameLogger.setLevel(GameLogger.Level.TRACE);
            GameLogger.setShowTimestamp(false);
        }

        @AfterEach
        void tearDown() {
            System.setErr(originalErr);
        }

        @Test
        @DisplayName("error should output to stderr")
        void errorShouldOutputToStderr() {
            logger.error("Error message");
            String output = errorStream.toString();
            assertTrue(output.contains("[ERROR]"));
            assertTrue(output.contains("Error message"));
        }

        @Test
        @DisplayName("warn should output to stderr")
        void warnShouldOutputToStderr() {
            logger.warn("Warning message");
            String output = errorStream.toString();
            assertTrue(output.contains("[WARN]"));
            assertTrue(output.contains("Warning message"));
        }

        @Test
        @DisplayName("error with arguments should format message")
        void errorWithArgumentsShouldFormatMessage() {
            logger.error("Failed to connect to {}", "server");
            String output = errorStream.toString();
            assertTrue(output.contains("Failed to connect to server"));
        }
    }

    @Nested
    @DisplayName("Level Filtering Tests")
    class LevelFilteringTests {

        private GameLogger logger;
        private ByteArrayOutputStream outputStream;
        private PrintStream originalOut;

        @BeforeEach
        void setUp() {
            logger = GameLogger.getLogger("Test");
            outputStream = new ByteArrayOutputStream();
            originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            GameLogger.setShowTimestamp(false);
        }

        @AfterEach
        void tearDown() {
            System.setOut(originalOut);
        }

        @Test
        @DisplayName("Should not log debug when level is INFO")
        void shouldNotLogDebugWhenLevelIsInfo() {
            GameLogger.setLevel(GameLogger.Level.INFO);
            logger.debug("Debug message");
            String output = outputStream.toString();
            assertFalse(output.contains("Debug message"));
        }

        @Test
        @DisplayName("Should not log info when level is WARN")
        void shouldNotLogInfoWhenLevelIsWarn() {
            GameLogger.setLevel(GameLogger.Level.WARN);
            logger.info("Info message");
            String output = outputStream.toString();
            assertFalse(output.contains("Info message"));
        }

        @Test
        @DisplayName("Should not log anything when level is OFF")
        void shouldNotLogAnythingWhenLevelIsOff() {
            GameLogger.setLevel(GameLogger.Level.OFF);
            logger.error("Error message");
            logger.info("Info message");
            String output = outputStream.toString();
            assertTrue(output.isEmpty());
        }
    }

    @Nested
    @DisplayName("Message Formatting Tests")
    class MessageFormattingTests {

        private GameLogger logger;
        private ByteArrayOutputStream outputStream;
        private PrintStream originalOut;

        @BeforeEach
        void setUp() {
            logger = GameLogger.getLogger("Test");
            outputStream = new ByteArrayOutputStream();
            originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            GameLogger.setLevel(GameLogger.Level.TRACE);
            GameLogger.setShowTimestamp(false);
        }

        @AfterEach
        void tearDown() {
            System.setOut(originalOut);
        }

        @Test
        @DisplayName("Should handle multiple placeholders")
        void shouldHandleMultiplePlaceholders() {
            logger.info("{} + {} = {}", 1, 2, 3);
            String output = outputStream.toString();
            assertTrue(output.contains("1 + 2 = 3"));
        }

        @Test
        @DisplayName("Should handle more placeholders than arguments")
        void shouldHandleMorePlaceholdersThanArguments() {
            logger.info("{} + {} = {}", 1, 2);
            String output = outputStream.toString();
            assertTrue(output.contains("1 + 2 = {}"));
        }

        @Test
        @DisplayName("Should handle no arguments")
        void shouldHandleNoArguments() {
            logger.info("Simple message");
            String output = outputStream.toString();
            assertTrue(output.contains("Simple message"));
        }

        @Test
        @DisplayName("Should handle null arguments array")
        void shouldHandleNullArgumentsArray() {
            logger.info("Message with {} placeholder", (Object[]) null);
            String output = outputStream.toString();
            assertTrue(output.contains("Message with {} placeholder"));
        }
    }
}
