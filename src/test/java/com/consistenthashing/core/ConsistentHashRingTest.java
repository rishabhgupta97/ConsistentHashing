package com.consistenthashing.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ConsistentHashRing class.
 * 
 * Tests cover:
 * - Basic functionality (add/remove servers, key lookup)
 * - Edge cases (null values, empty ring)
 * - Thread safety scenarios
 * - Load distribution validation
 * - Virtual nodes behavior
 */
@DisplayName("ConsistentHashRing Tests")
class ConsistentHashRingTest {

    private ConsistentHashRing<String> hashRing;

    @BeforeEach
    void setUp() {
        hashRing = new ConsistentHashRing<>(3); // 3 virtual nodes for testing
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ring with default virtual nodes")
        void shouldCreateRingWithDefaultVirtualNodes() {
            ConsistentHashRing<String> defaultRing = new ConsistentHashRing<>();
            assertNotNull(defaultRing);
            assertTrue(defaultRing.isEmpty());
        }

        @Test
        @DisplayName("Should create ring with custom virtual nodes")
        void shouldCreateRingWithCustomVirtualNodes() {
            ConsistentHashRing<String> customRing = new ConsistentHashRing<>(100);
            assertNotNull(customRing);
            assertTrue(customRing.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception for invalid virtual nodes")
        void shouldThrowExceptionForInvalidVirtualNodes() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ConsistentHashRing<>(-1));
            assertThrows(IllegalArgumentException.class,
                    () -> new ConsistentHashRing<>(0));
        }
    }

    @Nested
    @DisplayName("Server Management Tests")
    class ServerManagementTests {

        @Test
        @DisplayName("Should add server successfully")
        void shouldAddServerSuccessfully() {
            hashRing.addServer("server1");

            assertEquals(1, hashRing.getServerCount());
            assertEquals(3, hashRing.getTotalVirtualNodes()); // 3 virtual nodes per server
            assertFalse(hashRing.isEmpty());
            assertTrue(hashRing.getServers().contains("server1"));
        }

        @Test
        @DisplayName("Should add multiple servers")
        void shouldAddMultipleServers() {
            hashRing.addServer("server1");
            hashRing.addServer("server2");
            hashRing.addServer("server3");

            assertEquals(3, hashRing.getServerCount());
            assertEquals(9, hashRing.getTotalVirtualNodes()); // 3 servers * 3 virtual nodes

            Set<String> servers = hashRing.getServers();
            assertTrue(servers.contains("server1"));
            assertTrue(servers.contains("server2"));
            assertTrue(servers.contains("server3"));
        }

        @Test
        @DisplayName("Should remove server successfully")
        void shouldRemoveServerSuccessfully() {
            hashRing.addServer("server1");
            hashRing.addServer("server2");

            assertTrue(hashRing.removeServer("server1"));
            assertEquals(1, hashRing.getServerCount());
            assertEquals(3, hashRing.getTotalVirtualNodes());
            assertFalse(hashRing.getServers().contains("server1"));
            assertTrue(hashRing.getServers().contains("server2"));
        }

        @Test
        @DisplayName("Should return false when removing non-existent server")
        void shouldReturnFalseWhenRemovingNonExistentServer() {
            hashRing.addServer("server1");

            assertFalse(hashRing.removeServer("server2"));
            assertEquals(1, hashRing.getServerCount());
        }

        @Test
        @DisplayName("Should handle null server addition")
        void shouldHandleNullServerAddition() {
            assertThrows(IllegalArgumentException.class,
                    () -> hashRing.addServer(null));
        }

        @Test
        @DisplayName("Should handle null server removal")
        void shouldHandleNullServerRemoval() {
            assertFalse(hashRing.removeServer(null));
        }

        @Test
        @DisplayName("Should clear all servers")
        void shouldClearAllServers() {
            hashRing.addServer("server1");
            hashRing.addServer("server2");
            hashRing.addServer("server3");

            hashRing.clear();

            assertTrue(hashRing.isEmpty());
            assertEquals(0, hashRing.getServerCount());
            assertEquals(0, hashRing.getTotalVirtualNodes());
        }
    }

    @Nested
    @DisplayName("Key Lookup Tests")
    class KeyLookupTests {

        @Test
        @DisplayName("Should return null for empty ring")
        void shouldReturnNullForEmptyRing() {
            assertNull(hashRing.getServer("testkey"));
        }

        @Test
        @DisplayName("Should return server for key in single server ring")
        void shouldReturnServerForKeyInSingleServerRing() {
            hashRing.addServer("server1");

            assertEquals("server1", hashRing.getServer("testkey"));
            assertEquals("server1", hashRing.getServer("anotherkey"));
        }

        @Test
        @DisplayName("Should distribute keys across multiple servers")
        void shouldDistributeKeysAcrossMultipleServers() {
            hashRing.addServer("server1");
            hashRing.addServer("server2");
            hashRing.addServer("server3");

            List<String> testKeys = Arrays.asList(
                    "key1", "key2", "key3", "key4", "key5",
                    "user:john", "user:jane", "product:123", "session:abc", "cache:data");

            Set<String> usedServers = new java.util.HashSet<>();
            for (String key : testKeys) {
                String server = hashRing.getServer(key);
                assertNotNull(server);
                assertTrue(hashRing.getServers().contains(server));
                usedServers.add(server);
            }

            // With enough keys, we should use multiple servers
            assertTrue(usedServers.size() > 1, "Keys should be distributed across multiple servers");
        }

        @Test
        @DisplayName("Should handle null key lookup")
        void shouldHandleNullKeyLookup() {
            hashRing.addServer("server1");
            assertThrows(IllegalArgumentException.class,
                    () -> hashRing.getServer(null));
        }

        @Test
        @DisplayName("Should return consistent results for same key")
        void shouldReturnConsistentResultsForSameKey() {
            hashRing.addServer("server1");
            hashRing.addServer("server2");
            hashRing.addServer("server3");

            String key = "consistent-key";
            String firstResult = hashRing.getServer(key);

            // Multiple calls should return same result
            for (int i = 0; i < 10; i++) {
                assertEquals(firstResult, hashRing.getServer(key));
            }
        }
    }

    @Nested
    @DisplayName("Distribution Statistics Tests")
    class DistributionStatisticsTests {

        @Test
        @DisplayName("Should return empty stats for empty ring")
        void shouldReturnEmptyStatsForEmptyRing() {
            List<String> keys = Arrays.asList("key1", "key2", "key3");
            Map<String, Integer> stats = hashRing.getDistributionStats(keys);
            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("Should return distribution stats for multiple servers")
        void shouldReturnDistributionStatsForMultipleServers() {
            hashRing.addServer("server1");
            hashRing.addServer("server2");
            hashRing.addServer("server3");

            List<String> keys = Arrays.asList(
                    "key1", "key2", "key3", "key4", "key5", "key6", "key7", "key8", "key9", "key10");

            Map<String, Integer> stats = hashRing.getDistributionStats(keys);

            assertEquals(3, stats.size());
            assertTrue(stats.containsKey("server1"));
            assertTrue(stats.containsKey("server2"));
            assertTrue(stats.containsKey("server3"));

            int totalKeys = stats.values().stream().mapToInt(Integer::intValue).sum();
            assertEquals(keys.size(), totalKeys);
        }

        @Test
        @DisplayName("Should handle null keys list")
        void shouldHandleNullKeysList() {
            hashRing.addServer("server1");
            assertThrows(IllegalArgumentException.class,
                    () -> hashRing.getDistributionStats(null));
        }
    }

    @Nested
    @DisplayName("Scaling Tests")
    class ScalingTests {

        @Test
        @DisplayName("Should maintain key consistency when adding servers")
        void shouldMaintainKeyConsistencyWhenAddingServers() {
            // Start with 2 servers
            hashRing.addServer("server1");
            hashRing.addServer("server2");

            List<String> testKeys = Arrays.asList(
                    "key1", "key2", "key3", "key4", "key5", "key6", "key7", "key8", "key9", "key10");

            // Record initial mappings
            Map<String, String> initialMappings = new java.util.HashMap<>();
            for (String key : testKeys) {
                initialMappings.put(key, hashRing.getServer(key));
            }

            // Add a new server
            hashRing.addServer("server3");

            // Count how many keys changed
            int changedKeys = 0;
            for (String key : testKeys) {
                String newServer = hashRing.getServer(key);
                if (!newServer.equals(initialMappings.get(key))) {
                    changedKeys++;
                }
            }

            // In consistent hashing, only a fraction of keys should change
            assertTrue(changedKeys < testKeys.size(),
                    "Not all keys should change when adding a server");
            assertTrue(changedKeys > 0,
                    "Some keys should change when adding a server");
        }

        @Test
        @DisplayName("Should maintain key consistency when removing servers")
        void shouldMaintainKeyConsistencyWhenRemovingServers() {
            // Start with 3 servers
            hashRing.addServer("server1");
            hashRing.addServer("server2");
            hashRing.addServer("server3");

            List<String> testKeys = Arrays.asList(
                    "key1", "key2", "key3", "key4", "key5", "key6", "key7", "key8", "key9", "key10");

            // Record initial mappings
            Map<String, String> initialMappings = new java.util.HashMap<>();
            for (String key : testKeys) {
                initialMappings.put(key, hashRing.getServer(key));
            }

            // Remove a server
            hashRing.removeServer("server2");

            // Count how many keys changed
            int changedKeys = 0;
            for (String key : testKeys) {
                String newServer = hashRing.getServer(key);
                if (!newServer.equals(initialMappings.get(key))) {
                    changedKeys++;
                }
            }

            // Only keys that were on the removed server should change
            assertTrue(changedKeys < testKeys.size(),
                    "Not all keys should change when removing a server");
        }
    }

    @Nested
    @DisplayName("Virtual Nodes Tests")
    class VirtualNodesTests {

        @Test
        @DisplayName("Should create correct number of virtual nodes")
        void shouldCreateCorrectNumberOfVirtualNodes() {
            ConsistentHashRing<String> ring = new ConsistentHashRing<>(5);

            ring.addServer("server1");
            ring.addServer("server2");

            assertEquals(2, ring.getServerCount());
            assertEquals(10, ring.getTotalVirtualNodes()); // 2 servers * 5 virtual nodes
        }

        @Test
        @DisplayName("Should improve distribution with more virtual nodes")
        void shouldImproveDistributionWithMoreVirtualNodes() {
            // Test with few virtual nodes
            ConsistentHashRing<String> ringLowVNodes = new ConsistentHashRing<>(1);
            ringLowVNodes.addServer("server1");
            ringLowVNodes.addServer("server2");
            ringLowVNodes.addServer("server3");

            // Test with many virtual nodes
            ConsistentHashRing<String> ringHighVNodes = new ConsistentHashRing<>(50);
            ringHighVNodes.addServer("server1");
            ringHighVNodes.addServer("server2");
            ringHighVNodes.addServer("server3");

            List<String> testKeys = generateTestKeys(300);

            // Get distributions
            Map<String, Integer> lowVNodeDist = ringLowVNodes.getDistributionStats(testKeys);
            Map<String, Integer> highVNodeDist = ringHighVNodes.getDistributionStats(testKeys);

            // Calculate standard deviation for both
            double lowStdDev = calculateStandardDeviation(lowVNodeDist.values());
            double highStdDev = calculateStandardDeviation(highVNodeDist.values());

            // Higher virtual nodes should have better (lower) standard deviation
            assertTrue(highStdDev <= lowStdDev,
                    "Higher virtual nodes should provide better distribution");
        }

        private List<String> generateTestKeys(int count) {
            List<String> keys = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                keys.add("key:" + i + ":" + System.nanoTime());
            }
            return keys;
        }

        private double calculateStandardDeviation(java.util.Collection<Integer> values) {
            double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            double variance = values.stream()
                    .mapToDouble(val -> Math.pow(val - mean, 2))
                    .average().orElse(0.0);
            return Math.sqrt(variance);
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent server additions")
        void shouldHandleConcurrentServerAdditions() throws InterruptedException {
            final int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    hashRing.addServer("server" + threadId);
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

            assertEquals(threadCount, hashRing.getServerCount());
        }

        @Test
        @DisplayName("Should handle concurrent key lookups")
        void shouldHandleConcurrentKeyLookups() throws InterruptedException {
            hashRing.addServer("server1");
            hashRing.addServer("server2");
            hashRing.addServer("server3");

            final int threadCount = 10;
            final int lookupsPerThread = 100;
            Thread[] threads = new Thread[threadCount];
            final java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(
                    0);

            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < lookupsPerThread; j++) {
                        String key = "key:" + threadId + ":" + j;
                        String server = hashRing.getServer(key);
                        if (server != null) {
                            successCount.incrementAndGet();
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

            assertEquals(threadCount * lookupsPerThread, successCount.get());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle duplicate server additions")
        void shouldHandleDuplicateServerAdditions() {
            hashRing.addServer("server1");

            // Adding same server again should not increase count
            hashRing.addServer("server1");

            assertEquals(1, hashRing.getServerCount());
        }

        @Test
        @DisplayName("Should handle empty string keys")
        void shouldHandleEmptyStringKeys() {
            hashRing.addServer("server1");

            assertNotNull(hashRing.getServer(""));
            assertEquals("server1", hashRing.getServer(""));
        }

        @Test
        @DisplayName("Should handle very long keys")
        void shouldHandleVeryLongKeys() {
            hashRing.addServer("server1");

            StringBuilder longKey = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                longKey.append("a");
            }

            assertNotNull(hashRing.getServer(longKey.toString()));
        }

        @Test
        @DisplayName("Should handle special characters in keys")
        void shouldHandleSpecialCharactersInKeys() {
            hashRing.addServer("server1");

            String[] specialKeys = {
                    "key with spaces",
                    "key:with:colons",
                    "key-with-dashes",
                    "key_with_underscores",
                    "key.with.dots",
                    "key/with/slashes",
                    "key\\with\\backslashes",
                    "key@with@symbols",
                    "key#with#hash",
                    "key%with%percent"
            };

            for (String key : specialKeys) {
                assertNotNull(hashRing.getServer(key),
                        "Should handle key: " + key);
            }
        }
    }
}