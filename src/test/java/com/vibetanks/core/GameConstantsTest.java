package com.vibetanks.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameConstants Tests")
class GameConstantsTest {

    @Nested
    @DisplayName("Map Dimension Constants")
    class MapDimensionTests {

        @Test
        @DisplayName("MAP_SIZE should be 26 tiles")
        void mapSizeIs26() {
            assertEquals(26, GameConstants.MAP_SIZE);
        }

        @Test
        @DisplayName("TILE_SIZE should be 32 pixels")
        void tileSizeIs32() {
            assertEquals(32, GameConstants.TILE_SIZE);
        }

        @Test
        @DisplayName("Map pixel dimensions should be calculated correctly")
        void mapPixelDimensionsCorrect() {
            int expectedPixels = 26 * 32; // 832 pixels
            assertEquals(expectedPixels, GameConstants.MAP_SIZE * GameConstants.TILE_SIZE);
        }
    }

    @Nested
    @DisplayName("Enemy Settings Constants")
    class EnemySettingsTests {

        @Test
        @DisplayName("TOTAL_ENEMIES should be 20")
        void totalEnemiesIs20() {
            assertEquals(20, GameConstants.TOTAL_ENEMIES);
        }

        @Test
        @DisplayName("MAX_ENEMIES_ON_SCREEN should be 5")
        void maxEnemiesOnScreenIs5() {
            assertEquals(5, GameConstants.MAX_ENEMIES_ON_SCREEN);
        }

        @Test
        @DisplayName("Max enemies should be less than total enemies")
        void maxEnemiesLessThanTotal() {
            assertTrue(GameConstants.MAX_ENEMIES_ON_SCREEN < GameConstants.TOTAL_ENEMIES);
        }
    }

    @Nested
    @DisplayName("Player Start Position Tests")
    class PlayerStartPositionTests {

        @Test
        @DisplayName("Should have 4 player start positions")
        void hasFourPlayerPositions() {
            assertEquals(4, GameConstants.PLAYER_START_POSITIONS.length);
        }

        @Test
        @DisplayName("Each position should have X and Y coordinates")
        void eachPositionHasTwoCoordinates() {
            for (double[] pos : GameConstants.PLAYER_START_POSITIONS) {
                assertEquals(2, pos.length);
            }
        }

        @Test
        @DisplayName("Player 1 position should be at tile (8, 24)")
        void player1PositionCorrect() {
            double[] pos = GameConstants.PLAYER_START_POSITIONS[0];
            assertEquals(8 * 32, pos[0]);
            assertEquals(24 * 32, pos[1]);
        }

        @Test
        @DisplayName("Player 2 position should be at tile (16, 24)")
        void player2PositionCorrect() {
            double[] pos = GameConstants.PLAYER_START_POSITIONS[1];
            assertEquals(16 * 32, pos[0]);
            assertEquals(24 * 32, pos[1]);
        }

        @Test
        @DisplayName("Player 3 position should be at tile (9, 24)")
        void player3PositionCorrect() {
            double[] pos = GameConstants.PLAYER_START_POSITIONS[2];
            assertEquals(9 * 32, pos[0]);
            assertEquals(24 * 32, pos[1]);
        }

        @Test
        @DisplayName("Player 4 position should be at tile (15, 24)")
        void player4PositionCorrect() {
            double[] pos = GameConstants.PLAYER_START_POSITIONS[3];
            assertEquals(15 * 32, pos[0]);
            assertEquals(24 * 32, pos[1]);
        }

        @Test
        @DisplayName("All players should start on the same Y row (row 24)")
        void allPlayersOnSameRow() {
            double expectedY = 24 * 32;
            for (double[] pos : GameConstants.PLAYER_START_POSITIONS) {
                assertEquals(expectedY, pos[1]);
            }
        }

        @Test
        @DisplayName("All start positions should be within map bounds")
        void allPositionsWithinBounds() {
            double maxCoord = GameConstants.MAP_SIZE * GameConstants.TILE_SIZE;
            for (double[] pos : GameConstants.PLAYER_START_POSITIONS) {
                assertTrue(pos[0] >= 0 && pos[0] < maxCoord, "X should be within bounds");
                assertTrue(pos[1] >= 0 && pos[1] < maxCoord, "Y should be within bounds");
            }
        }
    }

    @Nested
    @DisplayName("getPlayerStartPosition Method Tests")
    class GetPlayerStartPositionTests {

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3})
        @DisplayName("Valid indices should return correct position")
        void validIndicesReturnCorrectPosition(int index) {
            double[] result = GameConstants.getPlayerStartPosition(index);
            double[] expected = GameConstants.PLAYER_START_POSITIONS[index];

            assertArrayEquals(expected, result);
        }

        @Test
        @DisplayName("Negative index should return player 1 position")
        void negativeIndexReturnsPlayer1() {
            double[] result = GameConstants.getPlayerStartPosition(-1);
            double[] expected = GameConstants.PLAYER_START_POSITIONS[0];

            assertArrayEquals(expected, result);
        }

        @Test
        @DisplayName("Index beyond range should return player 1 position")
        void indexBeyondRangeReturnsPlayer1() {
            double[] result = GameConstants.getPlayerStartPosition(4);
            double[] expected = GameConstants.PLAYER_START_POSITIONS[0];

            assertArrayEquals(expected, result);
        }

        @Test
        @DisplayName("Large index should return player 1 position")
        void largeIndexReturnsPlayer1() {
            double[] result = GameConstants.getPlayerStartPosition(100);
            double[] expected = GameConstants.PLAYER_START_POSITIONS[0];

            assertArrayEquals(expected, result);
        }

        @Test
        @DisplayName("Very negative index should return player 1 position")
        void veryNegativeIndexReturnsPlayer1() {
            double[] result = GameConstants.getPlayerStartPosition(-100);
            double[] expected = GameConstants.PLAYER_START_POSITIONS[0];

            assertArrayEquals(expected, result);
        }
    }

    @Nested
    @DisplayName("Base Position Tests")
    class BasePositionTests {

        @Test
        @DisplayName("BASE_X should be at tile 12")
        void baseXAtTile12() {
            assertEquals(12 * 32, GameConstants.BASE_X);
        }

        @Test
        @DisplayName("BASE_Y should be at tile 24")
        void baseYAtTile24() {
            assertEquals(24 * 32, GameConstants.BASE_Y);
        }

        @Test
        @DisplayName("Base should be centered horizontally on bottom row")
        void baseCenteredOnBottomRow() {
            // Base at tile 12 in a 26-tile map should be roughly centered
            double centerTile = GameConstants.MAP_SIZE / 2.0;
            assertTrue(Math.abs(12 - centerTile) < 2, "Base should be near horizontal center");
        }

        @Test
        @DisplayName("Base should be within map bounds")
        void baseWithinBounds() {
            double maxCoord = GameConstants.MAP_SIZE * GameConstants.TILE_SIZE;
            assertTrue(GameConstants.BASE_X >= 0 && GameConstants.BASE_X < maxCoord);
            assertTrue(GameConstants.BASE_Y >= 0 && GameConstants.BASE_Y < maxCoord);
        }
    }

    @Nested
    @DisplayName("Timing Constants Tests")
    class TimingConstantsTests {

        @Test
        @DisplayName("BASE_PROTECTION_TIME should be 1 minute at 60 FPS")
        void baseProtectionTimeIs1Minute() {
            assertEquals(3600, GameConstants.BASE_PROTECTION_TIME); // 60 * 60 = 3600
        }

        @Test
        @DisplayName("FLASH_DURATION should be 1 second at 60 FPS")
        void flashDurationIs1Second() {
            assertEquals(60, GameConstants.FLASH_DURATION);
        }

        @Test
        @DisplayName("FREEZE_TIME should be 10 seconds at 60 FPS")
        void freezeTimeIs10Seconds() {
            assertEquals(600, GameConstants.FREEZE_TIME); // 10 * 60 = 600
        }

        @Test
        @DisplayName("ENEMY_SPEED_BOOST_TIME should be 30 seconds at 60 FPS")
        void enemySpeedBoostTimeIs30Seconds() {
            assertEquals(1800, GameConstants.ENEMY_SPEED_BOOST_TIME); // 30 * 60 = 1800
        }

        @Test
        @DisplayName("UFO_MESSAGE_DURATION should be 3 seconds at 60 FPS")
        void ufoMessageDurationIs3Seconds() {
            assertEquals(180, GameConstants.UFO_MESSAGE_DURATION); // 3 * 60 = 180
        }

        @Test
        @DisplayName("VICTORY_DELAY should be 5 seconds at 60 FPS")
        void victoryDelayIs5Seconds() {
            assertEquals(300, GameConstants.VICTORY_DELAY); // 5 * 60 = 300
        }

        @Test
        @DisplayName("All timing constants should be positive")
        void allTimingConstantsPositive() {
            assertTrue(GameConstants.BASE_PROTECTION_TIME > 0);
            assertTrue(GameConstants.FLASH_DURATION > 0);
            assertTrue(GameConstants.FREEZE_TIME > 0);
            assertTrue(GameConstants.ENEMY_SPEED_BOOST_TIME > 0);
            assertTrue(GameConstants.UFO_MESSAGE_DURATION > 0);
            assertTrue(GameConstants.VICTORY_DELAY > 0);
        }
    }

    @Nested
    @DisplayName("Speed Boost Constants Tests")
    class SpeedBoostTests {

        @Test
        @DisplayName("ENEMY_TEAM_SPEED_BOOST should be 30%")
        void enemyTeamSpeedBoostIs30Percent() {
            assertEquals(0.3, GameConstants.ENEMY_TEAM_SPEED_BOOST, 0.001);
        }

        @Test
        @DisplayName("Speed boost should be positive")
        void speedBoostPositive() {
            assertTrue(GameConstants.ENEMY_TEAM_SPEED_BOOST > 0);
        }

        @Test
        @DisplayName("Speed boost should be less than 100%")
        void speedBoostLessThan100Percent() {
            assertTrue(GameConstants.ENEMY_TEAM_SPEED_BOOST < 1.0);
        }
    }

    @Nested
    @DisplayName("UFO Settings Tests")
    class UFOSettingsTests {

        @Test
        @DisplayName("UFO_SPAWN_CHANCE should be very small")
        void ufoSpawnChanceVerySmall() {
            // 0.35% per frame = avg ~4.8 seconds to spawn
            assertEquals(0.0035, GameConstants.UFO_SPAWN_CHANCE, 0.0001);
        }

        @Test
        @DisplayName("UFO spawn chance should be positive")
        void ufoSpawnChancePositive() {
            assertTrue(GameConstants.UFO_SPAWN_CHANCE > 0);
        }

        @Test
        @DisplayName("UFO spawn chance should be less than 1%")
        void ufoSpawnChanceLessThan1Percent() {
            assertTrue(GameConstants.UFO_SPAWN_CHANCE < 0.01);
        }
    }

    @Nested
    @DisplayName("Scoring Constants Tests")
    class ScoringConstantsTests {

        @Test
        @DisplayName("SCORE_REGULAR should be 1")
        void scoreRegularIs1() {
            assertEquals(1, GameConstants.SCORE_REGULAR);
        }

        @Test
        @DisplayName("SCORE_ARMORED should be 1")
        void scoreArmoredIs1() {
            assertEquals(1, GameConstants.SCORE_ARMORED);
        }

        @Test
        @DisplayName("SCORE_FAST should be 1")
        void scoreFastIs1() {
            assertEquals(1, GameConstants.SCORE_FAST);
        }

        @Test
        @DisplayName("SCORE_POWER should be 2")
        void scorePowerIs2() {
            assertEquals(2, GameConstants.SCORE_POWER);
        }

        @Test
        @DisplayName("SCORE_HEAVY should be 5")
        void scoreHeavyIs5() {
            assertEquals(5, GameConstants.SCORE_HEAVY);
        }

        @Test
        @DisplayName("SCORE_BOSS should be 10")
        void scoreBossIs10() {
            assertEquals(10, GameConstants.SCORE_BOSS);
        }

        @Test
        @DisplayName("All scores should be positive")
        void allScoresPositive() {
            assertTrue(GameConstants.SCORE_REGULAR > 0);
            assertTrue(GameConstants.SCORE_ARMORED > 0);
            assertTrue(GameConstants.SCORE_FAST > 0);
            assertTrue(GameConstants.SCORE_POWER > 0);
            assertTrue(GameConstants.SCORE_HEAVY > 0);
            assertTrue(GameConstants.SCORE_BOSS > 0);
        }

        @Test
        @DisplayName("BOSS score should be highest")
        void bossScoreHighest() {
            assertTrue(GameConstants.SCORE_BOSS >= GameConstants.SCORE_REGULAR);
            assertTrue(GameConstants.SCORE_BOSS >= GameConstants.SCORE_ARMORED);
            assertTrue(GameConstants.SCORE_BOSS >= GameConstants.SCORE_FAST);
            assertTrue(GameConstants.SCORE_BOSS >= GameConstants.SCORE_POWER);
            assertTrue(GameConstants.SCORE_BOSS >= GameConstants.SCORE_HEAVY);
        }
    }

    @Nested
    @DisplayName("getScoreForEnemyType Method Tests")
    class GetScoreForEnemyTypeTests {

        @Test
        @DisplayName("REGULAR should return SCORE_REGULAR")
        void regularReturnsCorrectScore() {
            assertEquals(GameConstants.SCORE_REGULAR,
                GameConstants.getScoreForEnemyType(Tank.EnemyType.REGULAR));
        }

        @Test
        @DisplayName("ARMORED should return SCORE_REGULAR (default)")
        void armoredReturnsDefaultScore() {
            assertEquals(GameConstants.SCORE_REGULAR,
                GameConstants.getScoreForEnemyType(Tank.EnemyType.ARMORED));
        }

        @Test
        @DisplayName("FAST should return SCORE_REGULAR (default)")
        void fastReturnsDefaultScore() {
            assertEquals(GameConstants.SCORE_REGULAR,
                GameConstants.getScoreForEnemyType(Tank.EnemyType.FAST));
        }

        @Test
        @DisplayName("POWER should return SCORE_POWER")
        void powerReturnsCorrectScore() {
            assertEquals(GameConstants.SCORE_POWER,
                GameConstants.getScoreForEnemyType(Tank.EnemyType.POWER));
        }

        @Test
        @DisplayName("HEAVY should return SCORE_HEAVY")
        void heavyReturnsCorrectScore() {
            assertEquals(GameConstants.SCORE_HEAVY,
                GameConstants.getScoreForEnemyType(Tank.EnemyType.HEAVY));
        }

        @Test
        @DisplayName("BOSS should return SCORE_BOSS")
        void bossReturnsCorrectScore() {
            assertEquals(GameConstants.SCORE_BOSS,
                GameConstants.getScoreForEnemyType(Tank.EnemyType.BOSS));
        }

        @ParameterizedTest
        @EnumSource(Tank.EnemyType.class)
        @DisplayName("All enemy types should return positive score")
        void allEnemyTypesReturnPositiveScore(Tank.EnemyType type) {
            assertTrue(GameConstants.getScoreForEnemyType(type) > 0);
        }
    }
}
