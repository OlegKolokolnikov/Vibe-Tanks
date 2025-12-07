package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NicknameManager Tests")
class NicknameManagerTest {

    private String originalNickname;

    @BeforeEach
    void saveOriginalNickname() {
        originalNickname = NicknameManager.getNickname();
    }

    @AfterEach
    void restoreOriginalNickname() {
        if (originalNickname != null) {
            NicknameManager.setNickname(originalNickname);
        } else {
            NicknameManager.clearNickname();
        }
    }

    @Nested
    @DisplayName("Set and Get Nickname Tests")
    class SetAndGetTests {

        @Test
        @DisplayName("setNickname should save nickname")
        void setNicknameShouldSave() {
            NicknameManager.setNickname("TestPlayer");

            assertEquals("TestPlayer", NicknameManager.getNickname());
        }

        @Test
        @DisplayName("setNickname should trim whitespace")
        void setNicknameShouldTrimWhitespace() {
            NicknameManager.setNickname("  SpacedName  ");

            assertEquals("SpacedName", NicknameManager.getNickname());
        }

        @Test
        @DisplayName("setNickname should truncate to max length")
        void setNicknameShouldTruncate() {
            String longName = "ThisIsAVeryLongNickname";
            NicknameManager.setNickname(longName);

            String saved = NicknameManager.getNickname();
            assertNotNull(saved);
            assertEquals(NicknameManager.getMaxLength(), saved.length());
        }

        @Test
        @DisplayName("setNickname with null should clear nickname")
        void setNicknameNullShouldClear() {
            NicknameManager.setNickname("TestPlayer");
            NicknameManager.setNickname(null);

            assertNull(NicknameManager.getNickname());
        }

        @Test
        @DisplayName("setNickname with empty string should clear nickname")
        void setNicknameEmptyShouldClear() {
            NicknameManager.setNickname("TestPlayer");
            NicknameManager.setNickname("");

            assertNull(NicknameManager.getNickname());
        }

        @Test
        @DisplayName("setNickname with whitespace only should clear nickname")
        void setNicknameWhitespaceShouldClear() {
            NicknameManager.setNickname("TestPlayer");
            NicknameManager.setNickname("   ");

            assertNull(NicknameManager.getNickname());
        }
    }

    @Nested
    @DisplayName("Clear Nickname Tests")
    class ClearNicknameTests {

        @Test
        @DisplayName("clearNickname should remove saved nickname")
        void clearNicknameShouldRemove() {
            NicknameManager.setNickname("TestPlayer");

            NicknameManager.clearNickname();

            assertNull(NicknameManager.getNickname());
        }

        @Test
        @DisplayName("clearNickname on empty should not throw")
        void clearNicknameOnEmptyShouldNotThrow() {
            NicknameManager.clearNickname(); // Clear first

            assertDoesNotThrow(() -> NicknameManager.clearNickname());
        }
    }

    @Nested
    @DisplayName("Display Name Tests")
    class DisplayNameTests {

        @Test
        @DisplayName("getDisplayName for local player with nickname returns nickname")
        void getDisplayNameLocalWithNickname() {
            NicknameManager.setNickname("MyName");

            String displayName = NicknameManager.getDisplayName(1, true);

            assertEquals("MyName", displayName);
        }

        @Test
        @DisplayName("getDisplayName for local player without nickname returns P+number")
        void getDisplayNameLocalWithoutNickname() {
            NicknameManager.clearNickname();

            String displayName = NicknameManager.getDisplayName(1, true);

            assertEquals("P1", displayName);
        }

        @Test
        @DisplayName("getDisplayName for non-local player returns P+number")
        void getDisplayNameNonLocal() {
            NicknameManager.setNickname("MyName"); // Even with nickname set

            String displayName = NicknameManager.getDisplayName(2, false);

            assertEquals("P2", displayName);
        }

        @Test
        @DisplayName("getDisplayName returns correct number for different players")
        void getDisplayNameDifferentPlayers() {
            NicknameManager.clearNickname();

            assertEquals("P1", NicknameManager.getDisplayName(1, false));
            assertEquals("P2", NicknameManager.getDisplayName(2, false));
            assertEquals("P3", NicknameManager.getDisplayName(3, false));
            assertEquals("P4", NicknameManager.getDisplayName(4, false));
        }
    }

    @Nested
    @DisplayName("Max Length Tests")
    class MaxLengthTests {

        @Test
        @DisplayName("getMaxLength should return positive value")
        void getMaxLengthShouldReturnPositive() {
            assertTrue(NicknameManager.getMaxLength() > 0);
        }

        @Test
        @DisplayName("getMaxLength should return 12")
        void getMaxLengthShouldReturn12() {
            assertEquals(12, NicknameManager.getMaxLength());
        }

        @Test
        @DisplayName("Nickname exactly at max length should be preserved")
        void nicknameAtMaxLengthPreserved() {
            String exactLength = "a".repeat(NicknameManager.getMaxLength());
            NicknameManager.setNickname(exactLength);

            assertEquals(exactLength, NicknameManager.getNickname());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Special characters should be preserved")
        void specialCharactersPreserved() {
            NicknameManager.setNickname("User_123!");

            assertEquals("User_123!", NicknameManager.getNickname());
        }

        @Test
        @DisplayName("Unicode characters should be preserved")
        void unicodeCharactersPreserved() {
            NicknameManager.setNickname("Player\u2605");

            assertEquals("Player\u2605", NicknameManager.getNickname());
        }

        @Test
        @DisplayName("Single character nickname should work")
        void singleCharacterNickname() {
            NicknameManager.setNickname("X");

            assertEquals("X", NicknameManager.getNickname());
        }
    }
}
