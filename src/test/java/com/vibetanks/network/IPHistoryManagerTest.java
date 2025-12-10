package com.vibetanks.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IPHistoryManager Tests")
class IPHistoryManagerTest {

    @BeforeEach
    void setUp() {
        // Clear all saved IPs before each test
        IPHistoryManager.clearAll();
    }

    @AfterEach
    void tearDown() {
        // Clean up after tests
        IPHistoryManager.clearAll();
    }

    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {

        @Test
        @DisplayName("getSavedIPs should return empty list initially")
        void getSavedIPsShouldReturnEmptyListInitially() {
            List<String> ips = IPHistoryManager.getSavedIPs();

            assertNotNull(ips);
            assertTrue(ips.isEmpty());
        }
    }

    @Nested
    @DisplayName("Add IP Tests")
    class AddIPTests {

        @Test
        @DisplayName("addIP should add new IP to list")
        void addIPShouldAddNewIP() {
            IPHistoryManager.addIP("192.168.1.1");

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertEquals(1, ips.size());
            assertEquals("192.168.1.1", ips.get(0));
        }

        @Test
        @DisplayName("addIP should add IP at beginning")
        void addIPShouldAddAtBeginning() {
            IPHistoryManager.addIP("192.168.1.1");
            IPHistoryManager.addIP("192.168.1.2");

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertEquals("192.168.1.2", ips.get(0));
            assertEquals("192.168.1.1", ips.get(1));
        }

        @Test
        @DisplayName("addIP should move existing IP to top")
        void addIPShouldMoveExistingToTop() {
            IPHistoryManager.addIP("192.168.1.1");
            IPHistoryManager.addIP("192.168.1.2");
            IPHistoryManager.addIP("192.168.1.1"); // Re-add first IP

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertEquals(2, ips.size());
            assertEquals("192.168.1.1", ips.get(0));
            assertEquals("192.168.1.2", ips.get(1));
        }

        @Test
        @DisplayName("addIP should trim whitespace")
        void addIPShouldTrimWhitespace() {
            IPHistoryManager.addIP("  192.168.1.1  ");

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertEquals("192.168.1.1", ips.get(0));
        }

        @Test
        @DisplayName("addIP should ignore null")
        void addIPShouldIgnoreNull() {
            IPHistoryManager.addIP(null);

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertTrue(ips.isEmpty());
        }

        @Test
        @DisplayName("addIP should ignore empty string")
        void addIPShouldIgnoreEmptyString() {
            IPHistoryManager.addIP("");

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertTrue(ips.isEmpty());
        }

        @Test
        @DisplayName("addIP should ignore whitespace-only string")
        void addIPShouldIgnoreWhitespaceOnly() {
            IPHistoryManager.addIP("   ");

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertTrue(ips.isEmpty());
        }

        @Test
        @DisplayName("addIP should limit to 10 IPs")
        void addIPShouldLimitToTenIPs() {
            for (int i = 1; i <= 15; i++) {
                IPHistoryManager.addIP("192.168.1." + i);
            }

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertEquals(10, ips.size());
            // Most recent should be first
            assertEquals("192.168.1.15", ips.get(0));
            // Oldest kept should be at index 9
            assertEquals("192.168.1.6", ips.get(9));
        }
    }

    @Nested
    @DisplayName("Remove IP Tests")
    class RemoveIPTests {

        @Test
        @DisplayName("removeIP should remove existing IP")
        void removeIPShouldRemoveExistingIP() {
            IPHistoryManager.addIP("192.168.1.1");
            IPHistoryManager.addIP("192.168.1.2");

            IPHistoryManager.removeIP("192.168.1.1");

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertEquals(1, ips.size());
            assertEquals("192.168.1.2", ips.get(0));
        }

        @Test
        @DisplayName("removeIP should handle non-existent IP gracefully")
        void removeIPShouldHandleNonExistent() {
            IPHistoryManager.addIP("192.168.1.1");

            IPHistoryManager.removeIP("192.168.1.99");

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertEquals(1, ips.size());
        }

        @Test
        @DisplayName("removeIP should handle null gracefully")
        void removeIPShouldHandleNull() {
            IPHistoryManager.addIP("192.168.1.1");

            assertDoesNotThrow(() -> IPHistoryManager.removeIP(null));

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertEquals(1, ips.size());
        }

        @Test
        @DisplayName("removeIP should trim whitespace when matching")
        void removeIPShouldTrimWhitespace() {
            IPHistoryManager.addIP("192.168.1.1");

            IPHistoryManager.removeIP("  192.168.1.1  ");

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertTrue(ips.isEmpty());
        }
    }

    @Nested
    @DisplayName("Clear All Tests")
    class ClearAllTests {

        @Test
        @DisplayName("clearAll should remove all IPs")
        void clearAllShouldRemoveAllIPs() {
            IPHistoryManager.addIP("192.168.1.1");
            IPHistoryManager.addIP("192.168.1.2");
            IPHistoryManager.addIP("192.168.1.3");

            IPHistoryManager.clearAll();

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertTrue(ips.isEmpty());
        }

        @Test
        @DisplayName("clearAll should handle empty list gracefully")
        void clearAllShouldHandleEmptyList() {
            assertDoesNotThrow(() -> IPHistoryManager.clearAll());

            List<String> ips = IPHistoryManager.getSavedIPs();
            assertTrue(ips.isEmpty());
        }
    }

    @Nested
    @DisplayName("Persistence Tests")
    class PersistenceTests {

        @Test
        @DisplayName("IPs should persist across getSavedIPs calls")
        void ipsShouldPersistAcrossCalls() {
            IPHistoryManager.addIP("192.168.1.1");

            List<String> ips1 = IPHistoryManager.getSavedIPs();
            List<String> ips2 = IPHistoryManager.getSavedIPs();

            assertEquals(ips1, ips2);
        }
    }
}
