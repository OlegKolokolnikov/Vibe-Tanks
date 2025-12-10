package com.vibetanks.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DedicatedServer Tests")
class DedicatedServerTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should accept port number")
        void constructorShouldAcceptPortNumber() {
            DedicatedServer server = new DedicatedServer(12345);
            assertNotNull(server);
        }

        @Test
        @DisplayName("Constructor should accept default port")
        void constructorShouldAcceptDefaultPort() {
            DedicatedServer server = new DedicatedServer(25565);
            assertNotNull(server);
        }
    }

    @Nested
    @DisplayName("Server Lifecycle Tests")
    class ServerLifecycleTests {

        @Test
        @DisplayName("stop should not throw when called on new server")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void stopShouldNotThrowWhenCalledOnNewServer() {
            DedicatedServer server = new DedicatedServer(25570);
            assertDoesNotThrow(server::stop);
        }

        @Test
        @DisplayName("stop should be idempotent")
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        void stopShouldBeIdempotent() {
            DedicatedServer server = new DedicatedServer(25571);
            assertDoesNotThrow(() -> {
                server.stop();
                server.stop();
                server.stop();
            });
        }
    }

    @Nested
    @DisplayName("Constants Tests")
    class ConstantsTests {

        @Test
        @DisplayName("Default port should be 25565")
        void defaultPortShouldBe25565() throws Exception {
            // Access private field via reflection
            var field = DedicatedServer.class.getDeclaredField("DEFAULT_PORT");
            field.setAccessible(true);
            assertEquals(25565, field.get(null));
        }

        @Test
        @DisplayName("Max players should be 4")
        void maxPlayersShouldBeFour() throws Exception {
            var field = DedicatedServer.class.getDeclaredField("MAX_PLAYERS");
            field.setAccessible(true);
            assertEquals(4, field.get(null));
        }

        @Test
        @DisplayName("Frame time should be approximately 60 FPS")
        void frameTimeShouldBeApproximately60Fps() throws Exception {
            var field = DedicatedServer.class.getDeclaredField("FRAME_TIME_NS");
            field.setAccessible(true);
            long frameTimeNs = (long) field.get(null);
            // 60 FPS = 16.67ms per frame = 16,666,667 ns
            assertTrue(frameTimeNs > 16_000_000 && frameTimeNs < 17_000_000);
        }
    }

    @Nested
    @DisplayName("Main Method Tests")
    class MainMethodTests {

        @Test
        @DisplayName("main method should exist")
        void mainMethodShouldExist() throws NoSuchMethodException {
            assertNotNull(DedicatedServer.class.getMethod("main", String[].class));
        }

        @Test
        @DisplayName("main method should be public static")
        void mainMethodShouldBePublicStatic() throws NoSuchMethodException {
            var method = DedicatedServer.class.getMethod("main", String[].class);
            assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()));
            assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
        }
    }

    @Nested
    @DisplayName("Method Existence Tests")
    class MethodExistenceTests {

        @Test
        @DisplayName("start method should exist")
        void startMethodShouldExist() throws NoSuchMethodException {
            assertNotNull(DedicatedServer.class.getMethod("start"));
        }

        @Test
        @DisplayName("stop method should exist")
        void stopMethodShouldExist() throws NoSuchMethodException {
            assertNotNull(DedicatedServer.class.getMethod("stop"));
        }
    }
}
