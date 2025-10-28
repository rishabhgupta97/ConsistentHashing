package com.consistenthashing.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CacheServer class.
 */
@DisplayName("CacheServer Tests")
class CacheServerTest {

    private CacheServer server;

    @BeforeEach
    void setUp() {
        server = new CacheServer("test-server");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create server with valid ID")
        void shouldCreateServerWithValidId() {
            CacheServer newServer = new CacheServer("server-1");

            assertEquals("server-1", newServer.getServerId());
            assertTrue(newServer.isActive());
            assertEquals(0, newServer.size());
        }

        @Test
        @DisplayName("Should handle server ID with spaces")
        void shouldHandleServerIdWithSpaces() {
            CacheServer newServer = new CacheServer("  server-1  ");
            assertEquals("server-1", newServer.getServerId());
        }

        @Test
        @DisplayName("Should throw exception for null server ID")
        void shouldThrowExceptionForNullServerId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CacheServer(null));
        }

        @Test
        @DisplayName("Should throw exception for empty server ID")
        void shouldThrowExceptionForEmptyServerId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CacheServer(""));
            assertThrows(IllegalArgumentException.class,
                    () -> new CacheServer("   "));
        }
    }

    @Nested
    @DisplayName("Basic Operations Tests")
    class BasicOperationsTests {

        @Test
        @DisplayName("Should put and get values")
        void shouldPutAndGetValues() {
            Object result = server.put("key1", "value1");
            assertNull(result); // No previous value

            Object retrieved = server.get("key1");
            assertEquals("value1", retrieved);
            assertEquals(1, server.size());
        }

        @Test
        @DisplayName("Should return previous value on put")
        void shouldReturnPreviousValueOnPut() {
            server.put("key1", "value1");
            Object previous = server.put("key1", "value2");

            assertEquals("value1", previous);
            assertEquals("value2", server.get("key1"));
            assertEquals(1, server.size()); // Size should not change
        }

        @Test
        @DisplayName("Should remove values")
        void shouldRemoveValues() {
            server.put("key1", "value1");
            Object removed = server.remove("key1");

            assertEquals("value1", removed);
            assertNull(server.get("key1"));
            assertEquals(0, server.size());
        }

        @Test
        @DisplayName("Should return null for non-existent keys")
        void shouldReturnNullForNonExistentKeys() {
            assertNull(server.get("non-existent"));
            assertNull(server.remove("non-existent"));
        }

        @Test
        @DisplayName("Should check key existence")
        void shouldCheckKeyExistence() {
            assertFalse(server.containsKey("key1"));

            server.put("key1", "value1");
            assertTrue(server.containsKey("key1"));

            server.remove("key1");
            assertFalse(server.containsKey("key1"));
        }

        @Test
        @DisplayName("Should handle null keys in operations")
        void shouldHandleNullKeysInOperations() {
            assertThrows(IllegalArgumentException.class, () -> server.get(null));
            assertThrows(IllegalArgumentException.class, () -> server.put(null, "value"));
            assertThrows(IllegalArgumentException.class, () -> server.remove(null));
            assertThrows(IllegalArgumentException.class, () -> server.containsKey(null));
        }

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            server.put("key1", null);
            assertNull(server.get("key1"));
            assertTrue(server.containsKey("key1"));
            assertEquals(1, server.size());
        }

        @Test
        @DisplayName("Should clear all data")
        void shouldClearAllData() {
            server.put("key1", "value1");
            server.put("key2", "value2");
            server.put("key3", "value3");

            assertEquals(3, server.size());

            server.clear();

            assertEquals(0, server.size());
            assertFalse(server.containsKey("key1"));
            assertFalse(server.containsKey("key2"));
            assertFalse(server.containsKey("key3"));
        }
    }

    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {

        @Test
        @DisplayName("Should track hit and miss counts")
        void shouldTrackHitAndMissCounts() {
            server.put("key1", "value1");

            // Hit
            server.get("key1");
            assertEquals(1, server.getHitCount());
            assertEquals(0, server.getMissCount());
            assertEquals(1, server.getRequestCount());

            // Miss
            server.get("key2");
            assertEquals(1, server.getHitCount());
            assertEquals(1, server.getMissCount());
            assertEquals(2, server.getRequestCount());
        }

        @Test
        @DisplayName("Should calculate hit rate correctly")
        void shouldCalculateHitRateCorrectly() {
            server.put("key1", "value1");

            // Initially no requests
            assertEquals(0.0, server.getHitRate());

            // 1 hit, 0 misses = 100%
            server.get("key1");
            assertEquals(100.0, server.getHitRate());

            // 1 hit, 1 miss = 50%
            server.get("key2");
            assertEquals(50.0, server.getHitRate());

            // 2 hits, 1 miss = 66.67%
            server.get("key1");
            assertEquals(66.67, server.getHitRate(), 0.01);
        }

        @Test
        @DisplayName("Should track data size correctly")
        void shouldTrackDataSizeCorrectly() {
            assertEquals(0, server.getDataSize());

            server.put("key1", "value1");
            assertEquals(1, server.getDataSize());

            server.put("key2", "value2");
            assertEquals(2, server.getDataSize());

            // Updating existing key should not change size
            server.put("key1", "new_value1");
            assertEquals(2, server.getDataSize());

            server.remove("key1");
            assertEquals(1, server.getDataSize());

            server.clear();
            assertEquals(0, server.getDataSize());
        }

        @Test
        @DisplayName("Should track uptime")
        void shouldTrackUptime() {
            long uptime1 = server.getUptime();
            // Perform some operations to let time pass
            server.put("key1", "value1");
            server.get("key1");
            long uptime2 = server.getUptime();

            assertTrue(uptime2 >= uptime1);
        }

        @Test
        @DisplayName("Should reset metrics")
        void shouldResetMetrics() {
            server.put("key1", "value1");
            server.get("key1"); // hit
            server.get("key2"); // miss

            assertTrue(server.getRequestCount() > 0);
            assertTrue(server.getHitCount() > 0);
            assertTrue(server.getMissCount() > 0);

            server.resetMetrics();

            assertEquals(0, server.getRequestCount());
            assertEquals(0, server.getHitCount());
            assertEquals(0, server.getMissCount());
            assertEquals(0.0, server.getHitRate());
        }
    }

    @Nested
    @DisplayName("Server Status Tests")
    class ServerStatusTests {

        @Test
        @DisplayName("Should be active by default")
        void shouldBeActiveByDefault() {
            assertTrue(server.isActive());
        }

        @Test
        @DisplayName("Should allow status changes")
        void shouldAllowStatusChanges() {
            server.setActive(false);
            assertFalse(server.isActive());

            server.setActive(true);
            assertTrue(server.isActive());
        }

        @Test
        @DisplayName("Should simulate failure")
        void shouldSimulateFailure() {
            server.simulateFailure();
            assertFalse(server.isActive());
        }

        @Test
        @DisplayName("Should recover from failure")
        void shouldRecoverFromFailure() {
            server.simulateFailure();
            assertFalse(server.isActive());

            server.recover();
            assertTrue(server.isActive());
        }

        @Test
        @DisplayName("Should throw exception when operations on inactive server")
        void shouldThrowExceptionWhenOperationsOnInactiveServer() {
            server.setActive(false);

            assertThrows(IllegalStateException.class, () -> server.get("key1"));
            assertThrows(IllegalStateException.class, () -> server.put("key1", "value"));
            assertThrows(IllegalStateException.class, () -> server.remove("key1"));
            assertThrows(IllegalStateException.class, () -> server.containsKey("key1"));
            assertThrows(IllegalStateException.class, () -> server.clear());
            assertThrows(IllegalStateException.class, () -> server.getKeys());
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should provide comprehensive statistics")
        void shouldProvideComprehensiveStatistics() {
            server.put("key1", "value1");
            server.get("key1"); // hit
            server.get("key2"); // miss

            CacheServer.ServerStats stats = server.getStats();

            assertEquals("test-server", stats.getServerId());
            assertTrue(stats.isActive());
            assertEquals(1, stats.getHitCount());
            assertEquals(1, stats.getMissCount());
            assertEquals(2, stats.getRequestCount());
            assertEquals(1, stats.getDataSize());
            assertEquals(50.0, stats.getHitRate());
            assertTrue(stats.getUptime() > 0);
        }

        @Test
        @DisplayName("Should create statistics for inactive server")
        void shouldCreateStatisticsForInactiveServer() {
            server.setActive(false);

            CacheServer.ServerStats stats = server.getStats();

            assertEquals("test-server", stats.getServerId());
            assertFalse(stats.isActive());
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal based on server ID")
        void shouldBeEqualBasedOnServerId() {
            CacheServer server1 = new CacheServer("server-1");
            CacheServer server2 = new CacheServer("server-1");
            CacheServer server3 = new CacheServer("server-2");

            assertEquals(server1, server2);
            assertNotEquals(server1, server3);
            assertEquals(server1.hashCode(), server2.hashCode());
        }

        @Test
        @DisplayName("Should handle null in equals")
        void shouldHandleNullInEquals() {
            assertNotEquals(null, server);
            assertEquals(server, server); // reflexive
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent operations")
        void shouldHandleConcurrentOperations() throws InterruptedException {
            final int threadCount = 10;
            final int operationsPerThread = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key:" + threadId + ":" + j;
                        String value = "value:" + threadId + ":" + j;

                        server.put(key, value);
                        server.get(key);
                        if (j % 10 == 0) {
                            server.remove(key);
                        }
                    }
                });
            }

            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            // Verify server is still functional
            assertTrue(server.getRequestCount() > 0);
            assertTrue(server.isActive());
        }
    }
}