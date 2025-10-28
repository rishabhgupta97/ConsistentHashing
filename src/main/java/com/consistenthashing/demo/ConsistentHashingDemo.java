package com.consistenthashing.demo;

import com.consistenthashing.cache.DistributedCache;
import com.consistenthashing.server.CacheServer;
import com.consistenthashing.core.ConsistentHashRing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Comprehensive demonstration of Consistent Hashing implementation.
 * 
 * This demo showcases various scenarios including:
 * - Basic cache operations
 * - Server scaling (addition/removal)
 * - Load distribution analysis
 * - Server failure and recovery simulation
 * - Performance comparison with simple modulo hashing
 * 
 * Run this class to see consistent hashing in action!
 * 
 * @author Your Name
 * @version 1.0
 */
public class ConsistentHashingDemo {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String SUBSEPARATOR = "-".repeat(40);

    public static void main(String[] args) {
        System.out.println(SEPARATOR);
        System.out.println("🚀 CONSISTENT HASHING DEMONSTRATION");
        System.out.println(SEPARATOR);

        ConsistentHashingDemo demo = new ConsistentHashingDemo();

        // Run all demonstrations
        demo.basicOperationsDemo();
        demo.loadDistributionDemo();
        demo.serverScalingDemo();
        demo.serverFailureDemo();
        demo.performanceComparisonDemo();

        System.out.println(SEPARATOR);
        System.out.println("✅ All demonstrations completed successfully!");
        System.out.println("💡 Check the output above to understand how consistent hashing works.");
        System.out.println(SEPARATOR);
    }

    /**
     * Demonstrates basic cache operations
     */
    private void basicOperationsDemo() {
        System.out.println("\n📝 DEMO 1: BASIC CACHE OPERATIONS");
        System.out.println(SUBSEPARATOR);

        DistributedCache cache = new DistributedCache();

        // Add servers
        System.out.println("Adding cache servers...");
        cache.addServer("server-A");
        cache.addServer("server-B");
        cache.addServer("server-C");

        System.out.println("✓ Added 3 servers: server-A, server-B, server-C");

        // Store some data
        System.out.println("\nStoring user data...");
        cache.put("user:john", "John Doe, Age: 30");
        cache.put("user:jane", "Jane Smith, Age: 25");
        cache.put("user:bob", "Bob Johnson, Age: 35");
        cache.put("user:alice", "Alice Brown, Age: 28");
        cache.put("user:charlie", "Charlie Wilson, Age: 32");

        System.out.println("✓ Stored 5 user records");

        // Retrieve data
        System.out.println("\nRetrieving data:");
        String[] users = { "user:john", "user:jane", "user:bob", "user:alice", "user:charlie" };

        for (String user : users) {
            String data = (String) cache.get(user);
            CacheServer server = cache.getServer(determineServerId(cache, user));
            System.out.printf("  %s -> %s (stored on %s)\n",
                    user, data, server != null ? server.getServerId() : "unknown");
        }

        // Show cache statistics
        System.out.println("\nCache Statistics:");
        printCacheStats(cache);
    }

    /**
     * Demonstrates load distribution across servers
     */
    private void loadDistributionDemo() {
        System.out.println("\n⚖️ DEMO 2: LOAD DISTRIBUTION ANALYSIS");
        System.out.println(SUBSEPARATOR);

        DistributedCache cache = new DistributedCache();

        // Add servers
        cache.addServer("server-1");
        cache.addServer("server-2");
        cache.addServer("server-3");
        cache.addServer("server-4");

        System.out.println("Added 4 servers for load distribution test");

        // Generate many keys to test distribution
        List<String> testKeys = generateTestKeys(1000);

        // Store all keys
        System.out.println("Storing 1000 test keys...");
        for (int i = 0; i < testKeys.size(); i++) {
            cache.put(testKeys.get(i), "value-" + i);
        }

        // Analyze distribution
        Map<String, Integer> distribution = cache.getDistributionStats(testKeys);

        System.out.println("\nKey Distribution Analysis:");
        System.out.println("Server ID       | Keys Count | Percentage");
        System.out.println("----------------|------------|----------");

        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            double percentage = (double) entry.getValue() / testKeys.size() * 100;
            System.out.printf("%-15s | %-10d | %.2f%%\n",
                    entry.getKey(), entry.getValue(), percentage);
        }

        // Calculate standard deviation to show uniformity
        double mean = testKeys.size() / 4.0;
        double variance = distribution.values().stream()
                .mapToDouble(count -> Math.pow(count - mean, 2))
                .average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        System.out.printf("\nDistribution Statistics:\n");
        System.out.printf("  Mean keys per server: %.2f\n", mean);
        System.out.printf("  Standard deviation: %.2f\n", stdDev);
        System.out.printf("  Distribution quality: %s\n",
                stdDev < mean * 0.1 ? "Excellent" : stdDev < mean * 0.2 ? "Good" : "Fair");
    }

    /**
     * Demonstrates server scaling (adding/removing servers)
     */
    private void serverScalingDemo() {
        System.out.println("\n🔧 DEMO 3: SERVER SCALING OPERATIONS");
        System.out.println(SUBSEPARATOR);

        DistributedCache cache = new DistributedCache();

        // Start with 3 servers
        System.out.println("Initial setup: Adding 3 servers");
        cache.addServer("server-A");
        cache.addServer("server-B");
        cache.addServer("server-C");

        // Add initial data
        List<String> testKeys = generateTestKeys(100);
        System.out.println("Storing 100 keys...");

        for (int i = 0; i < testKeys.size(); i++) {
            cache.put(testKeys.get(i), "data-" + i);
        }

        System.out.println("\nInitial distribution:");
        printDistribution(cache, testKeys);

        // Scale up: Add a new server
        System.out.println("\n🔼 SCALING UP: Adding server-D");
        cache.addServer("server-D");

        System.out.println("Distribution after adding server-D:");
        printDistribution(cache, testKeys);

        // Scale down: Remove a server
        System.out.println("\n🔽 SCALING DOWN: Removing server-B");
        boolean removed = cache.removeServer("server-B");
        System.out.println("Server removal " + (removed ? "successful" : "failed"));

        System.out.println("Distribution after removing server-B:");
        printDistribution(cache, testKeys);

        // Show migration statistics
        DistributedCache.CacheStats stats = cache.getStats();
        System.out.printf("\nKey migrations performed: %d\n", stats.getKeyMigrations());
        System.out.println("💡 Notice how only affected keys were redistributed!");
    }

    /**
     * Demonstrates server failure and recovery scenarios
     */
    private void serverFailureDemo() {
        System.out.println("\n🚨 DEMO 4: SERVER FAILURE AND RECOVERY");
        System.out.println(SUBSEPARATOR);

        DistributedCache cache = new DistributedCache();

        // Setup servers and data
        cache.addServer("server-1");
        cache.addServer("server-2");
        cache.addServer("server-3");

        System.out.println("Setup: 3 servers with test data");

        // Add test data
        String[] testData = {
                "user:john", "user:jane", "user:bob", "user:alice", "user:charlie"
        };

        for (String key : testData) {
            cache.put(key, "Data for " + key);
        }

        System.out.println("Stored test data successfully");

        // Show initial state
        System.out.println("\nInitial state - all servers active:");
        printServerHealth(cache);

        // Simulate server failure
        System.out.println("\n💥 SIMULATING FAILURE: server-2 goes down");
        cache.simulateServerFailure("server-2");

        System.out.println("Server health after failure:");
        printServerHealth(cache);

        // Test data accessibility
        System.out.println("\nTesting data accessibility after server failure:");
        for (String key : testData) {
            Object value = cache.get(key);
            System.out.printf("  %s: %s\n", key, value != null ? "✓ Available" : "✗ Lost");
        }

        // Recover server
        System.out.println("\n🔄 RECOVERING server-2");
        cache.recoverServer("server-2");

        System.out.println("Server health after recovery:");
        printServerHealth(cache);

        System.out.println("\n💡 In consistent hashing, server failures affect minimal data!");
    }

    /**
     * Compares consistent hashing with simple modulo hashing
     */
    private void performanceComparisonDemo() {
        System.out.println("\n📊 DEMO 5: PERFORMANCE COMPARISON");
        System.out.println(SUBSEPARATOR);

        System.out.println("Comparing Consistent Hashing vs Simple Modulo Hashing");
        System.out.println("Scenario: Remove 1 server from 4 servers with 1000 keys");

        List<String> testKeys = generateTestKeys(1000);

        // Test consistent hashing
        System.out.println("\n🔄 Testing Consistent Hashing:");
        int consistentHashRemappings = testConsistentHashing(testKeys);

        // Test simple modulo hashing
        System.out.println("\n🔄 Testing Simple Modulo Hashing:");
        int moduloRemappings = testModuloHashing(testKeys);

        // Compare results
        System.out.println("\n📈 COMPARISON RESULTS:");
        System.out.printf("  Consistent Hashing remappings: %d (%.2f%%)\n",
                consistentHashRemappings, (double) consistentHashRemappings / testKeys.size() * 100);
        System.out.printf("  Simple Modulo remappings: %d (%.2f%%)\n",
                moduloRemappings, (double) moduloRemappings / testKeys.size() * 100);

        double improvement = (double) (moduloRemappings - consistentHashRemappings) / moduloRemappings * 100;
        System.out.printf("  Improvement: %.2f%% fewer remappings with Consistent Hashing\n", improvement);

        System.out.println("\n💡 Consistent Hashing significantly reduces key remapping during scaling!");
    }

    /**
     * Tests consistent hashing remapping behavior
     */
    private int testConsistentHashing(List<String> keys) {
        // Setup with 4 servers
        ConsistentHashRing<String> ring = new ConsistentHashRing<>();
        ring.addServer("server-1");
        ring.addServer("server-2");
        ring.addServer("server-3");
        ring.addServer("server-4");

        // Record initial mappings
        Map<String, String> initialMappings = new java.util.HashMap<>();
        for (String key : keys) {
            initialMappings.put(key, ring.getServer(key));
        }

        // Remove one server
        ring.removeServer("server-2");

        // Count remappings
        int remappings = 0;
        for (String key : keys) {
            String newServer = ring.getServer(key);
            if (!newServer.equals(initialMappings.get(key))) {
                remappings++;
            }
        }

        return remappings;
    }

    /**
     * Tests simple modulo hashing remapping behavior
     */
    private int testModuloHashing(List<String> keys) {
        String[] servers = { "server-1", "server-2", "server-3", "server-4" };
        String[] serversAfterRemoval = { "server-1", "server-3", "server-4" }; // Removed server-2

        // Record initial mappings
        Map<String, String> initialMappings = new java.util.HashMap<>();
        for (String key : keys) {
            int serverIndex = Math.abs(key.hashCode()) % servers.length;
            initialMappings.put(key, servers[serverIndex]);
        }

        // Count remappings after server removal
        int remappings = 0;
        for (String key : keys) {
            int newServerIndex = Math.abs(key.hashCode()) % serversAfterRemoval.length;
            String newServer = serversAfterRemoval[newServerIndex];

            if (!newServer.equals(initialMappings.get(key))) {
                remappings++;
            }
        }

        return remappings;
    }

    // Helper methods

    private List<String> generateTestKeys(int count) {
        List<String> keys = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducible results

        for (int i = 0; i < count; i++) {
            keys.add("key:" + i + ":" + random.nextInt(10000));
        }

        return keys;
    }

    private void printCacheStats(DistributedCache cache) {
        DistributedCache.CacheStats stats = cache.getStats();
        System.out.printf("  Total servers: %d (Active: %d)\n",
                stats.getTotalServers(), stats.getActiveServers());
        System.out.printf("  Total keys: %d\n", stats.getTotalKeys());
        System.out.printf("  Total requests: %d\n", stats.getTotalRequests());
        System.out.printf("  Hit rate: %.2f%%\n", stats.getOverallHitRate());
    }

    private void printDistribution(DistributedCache cache, List<String> keys) {
        Map<String, Integer> distribution = cache.getDistributionStats(keys);
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            double percentage = (double) entry.getValue() / keys.size() * 100;
            System.out.printf("  %s: %d keys (%.1f%%)\n",
                    entry.getKey(), entry.getValue(), percentage);
        }
    }

    private void printServerHealth(DistributedCache cache) {
        Map<String, CacheServer> servers = cache.getAllServers();
        for (CacheServer server : servers.values()) {
            String status = server.isActive() ? "🟢 Active" : "🔴 Failed";
            System.out.printf("  %s: %s (Keys: %d)\n",
                    server.getServerId(), status, server.size());
        }
    }

    private String determineServerId(DistributedCache cache, String key) {
        // This is a simplified way to determine which server a key maps to
        // In the actual implementation, this happens inside the hash ring
        Map<String, CacheServer> servers = cache.getAllServers();
        for (CacheServer server : servers.values()) {
            if (server.containsKey(key)) {
                return server.getServerId();
            }
        }
        return "unknown";
    }
}