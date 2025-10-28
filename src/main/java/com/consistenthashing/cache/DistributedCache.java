package com.consistenthashing.cache;

import com.consistenthashing.core.ConsistentHashRing;
import com.consistenthashing.server.CacheServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * A distributed cache implementation using consistent hashing.
 * 
 * This class provides a high-level interface for a distributed caching system
 * that uses consistent hashing to distribute keys across multiple cache
 * servers.
 * It handles server addition/removal, automatic key redistribution, and
 * provides
 * comprehensive statistics and monitoring capabilities.
 * 
 * Features:
 * - Automatic key distribution using consistent hashing
 * - Dynamic server addition and removal
 * - Key migration during topology changes
 * - Comprehensive statistics and monitoring
 * - Thread-safe operations
 * - Server failure simulation and recovery
 * 
 * @author Your Name
 * @version 1.0
 */
public class DistributedCache {

    private final ConsistentHashRing<CacheServer> hashRing;
    private final Map<String, CacheServer> servers;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Statistics
    private long totalRequests = 0;
    private long totalHits = 0;
    private long totalMisses = 0;
    private long keyMigrations = 0;

    /**
     * Creates a new distributed cache with default settings
     */
    public DistributedCache() {
        this(150); // Default 150 virtual nodes per server
    }

    /**
     * Creates a new distributed cache with specified virtual nodes
     * 
     * @param virtualNodesPerServer Number of virtual nodes per server
     */
    public DistributedCache(int virtualNodesPerServer) {
        this.hashRing = new ConsistentHashRing<>(virtualNodesPerServer);
        this.servers = new ConcurrentHashMap<>();
    }

    /**
     * Adds a cache server to the distributed cache
     * 
     * @param serverId Unique identifier for the server
     * @return The created CacheServer instance
     * @throws IllegalArgumentException if serverId is null, empty, or already
     *                                  exists
     */
    public CacheServer addServer(String serverId) {
        if (serverId == null || serverId.trim().isEmpty()) {
            throw new IllegalArgumentException("Server ID cannot be null or empty");
        }

        lock.writeLock().lock();
        try {
            if (servers.containsKey(serverId)) {
                throw new IllegalArgumentException("Server with ID '" + serverId + "' already exists");
            }

            CacheServer server = new CacheServer(serverId);
            servers.put(serverId, server);
            hashRing.addServer(server);

            return server;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a cache server from the distributed cache
     * 
     * @param serverId The ID of the server to remove
     * @return true if server was removed, false if server didn't exist
     */
    public boolean removeServer(String serverId) {
        if (serverId == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            CacheServer server = servers.get(serverId);
            if (server == null) {
                return false;
            }

            // Get all keys from the server before removing it
            Set<String> keysToMigrate = new HashSet<>(server.getKeys());

            // Remove server from hash ring and server map
            servers.remove(serverId);
            hashRing.removeServer(server);

            // Migrate keys to new servers
            migrateKeys(keysToMigrate, server);

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves a value from the distributed cache
     * 
     * @param key The key to lookup
     * @return The value associated with the key, null if not found
     * @throws IllegalArgumentException if key is null
     */
    public Object get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        lock.readLock().lock();
        try {
            totalRequests++;

            CacheServer server = hashRing.getServer(key);
            if (server == null || !server.isActive()) {
                totalMisses++;
                return null;
            }

            try {
                Object value = server.get(key);
                if (value != null) {
                    totalHits++;
                } else {
                    totalMisses++;
                }
                return value;
            } catch (IllegalStateException e) {
                // Server became inactive during operation
                totalMisses++;
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Stores a key-value pair in the distributed cache
     * 
     * @param key   The key to store
     * @param value The value to associate with the key
     * @return The previous value associated with the key, null if none
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException    if no active servers available
     */
    public Object put(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        lock.readLock().lock();
        try {
            CacheServer server = hashRing.getServer(key);
            if (server == null) {
                throw new IllegalStateException("No active servers available");
            }

            if (!server.isActive()) {
                throw new IllegalStateException("Target server is not active");
            }

            return server.put(key, value);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes a key-value pair from the distributed cache
     * 
     * @param key The key to remove
     * @return The value that was associated with the key, null if none
     * @throws IllegalArgumentException if key is null
     */
    public Object remove(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        lock.readLock().lock();
        try {
            CacheServer server = hashRing.getServer(key);
            if (server == null || !server.isActive()) {
                return null;
            }

            try {
                return server.remove(key);
            } catch (IllegalStateException e) {
                // Server became inactive during operation
                return null;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if a key exists in the distributed cache
     * 
     * @param key The key to check
     * @return true if key exists, false otherwise
     * @throws IllegalArgumentException if key is null
     */
    public boolean containsKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        lock.readLock().lock();
        try {
            CacheServer server = hashRing.getServer(key);
            if (server == null || !server.isActive()) {
                return false;
            }

            try {
                return server.containsKey(key);
            } catch (IllegalStateException e) {
                // Server became inactive during operation
                return false;
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a specific cache server by ID
     * 
     * @param serverId The server ID
     * @return The CacheServer instance, null if not found
     */
    public CacheServer getServer(String serverId) {
        lock.readLock().lock();
        try {
            return servers.get(serverId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all cache servers
     * 
     * @return Map of server ID to CacheServer
     */
    public Map<String, CacheServer> getAllServers() {
        lock.readLock().lock();
        try {
            return new HashMap<>(servers);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the number of active servers
     * 
     * @return Number of active servers
     */
    public int getActiveServerCount() {
        lock.readLock().lock();
        try {
            return (int) servers.values().stream()
                    .filter(CacheServer::isActive)
                    .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the total number of servers (active and inactive)
     * 
     * @return Total number of servers
     */
    public int getTotalServerCount() {
        lock.readLock().lock();
        try {
            return servers.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets comprehensive cache statistics
     * 
     * @return CacheStats object containing all metrics
     */
    public CacheStats getStats() {
        lock.readLock().lock();
        try {
            List<CacheServer.ServerStats> serverStats = servers.values().stream()
                    .map(CacheServer::getStats)
                    .collect(Collectors.toList());

            double overallHitRate = totalRequests > 0 ? (double) totalHits / totalRequests * 100.0 : 0.0;

            int totalKeys = servers.values().stream()
                    .mapToInt(CacheServer::size)
                    .sum();

            return CacheStats.builder()
                    .totalServers(servers.size())
                    .activeServers(getActiveServerCount())
                    .totalKeys(totalKeys)
                    .totalRequests(totalRequests)
                    .totalHits(totalHits)
                    .totalMisses(totalMisses)
                    .overallHitRate(overallHitRate)
                    .keyMigrations(keyMigrations)
                    .serverStats(serverStats)
                    .build();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets distribution statistics for a set of keys
     * 
     * @param keys List of keys to analyze
     * @return Map of server ID to number of keys assigned to it
     */
    public Map<String, Integer> getDistributionStats(List<String> keys) {
        lock.readLock().lock();
        try {
            Map<CacheServer, Integer> serverDistribution = hashRing.getDistributionStats(keys);

            return serverDistribution.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().getServerId(),
                            Map.Entry::getValue));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Simulates server failure
     * 
     * @param serverId The ID of the server to fail
     * @return true if server was failed, false if server doesn't exist
     */
    public boolean simulateServerFailure(String serverId) {
        lock.readLock().lock();
        try {
            CacheServer server = servers.get(serverId);
            if (server == null) {
                return false;
            }

            server.simulateFailure();
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Recovers a failed server
     * 
     * @param serverId The ID of the server to recover
     * @return true if server was recovered, false if server doesn't exist
     */
    public boolean recoverServer(String serverId) {
        lock.readLock().lock();
        try {
            CacheServer server = servers.get(serverId);
            if (server == null) {
                return false;
            }

            server.recover();
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears all data from all servers
     */
    public void clearAll() {
        lock.readLock().lock();
        try {
            servers.values().forEach(server -> {
                if (server.isActive()) {
                    server.clear();
                }
            });

            // Reset global statistics
            totalRequests = 0;
            totalHits = 0;
            totalMisses = 0;
            keyMigrations = 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Migrates keys from a removed server to appropriate new servers
     */
    private void migrateKeys(Set<String> keys, CacheServer removedServer) {
        for (String key : keys) {
            try {
                Object value = removedServer.get(key);
                if (value != null) {
                    CacheServer newServer = hashRing.getServer(key);
                    if (newServer != null && newServer.isActive()) {
                        newServer.put(key, value);
                        keyMigrations++;
                    }
                }
            } catch (Exception e) {
                // In production code, use proper logging framework
                // Silently handle migration failures for now
            }
        }
    }

    /**
     * Immutable class representing cache statistics
     */
    public static class CacheStats {
        private final int totalServers;
        private final int activeServers;
        private final int totalKeys;
        private final long totalRequests;
        private final long totalHits;
        private final long totalMisses;
        private final double overallHitRate;
        private final long keyMigrations;
        private final List<CacheServer.ServerStats> serverStats;

        private CacheStats(Builder builder) {
            this.totalServers = builder.totalServers;
            this.activeServers = builder.activeServers;
            this.totalKeys = builder.totalKeys;
            this.totalRequests = builder.totalRequests;
            this.totalHits = builder.totalHits;
            this.totalMisses = builder.totalMisses;
            this.overallHitRate = builder.overallHitRate;
            this.keyMigrations = builder.keyMigrations;
            this.serverStats = new ArrayList<>(builder.serverStats);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int totalServers;
            private int activeServers;
            private int totalKeys;
            private long totalRequests;
            private long totalHits;
            private long totalMisses;
            private double overallHitRate;
            private long keyMigrations;
            private List<CacheServer.ServerStats> serverStats = new ArrayList<>();

            public Builder totalServers(int totalServers) {
                this.totalServers = totalServers;
                return this;
            }

            public Builder activeServers(int activeServers) {
                this.activeServers = activeServers;
                return this;
            }

            public Builder totalKeys(int totalKeys) {
                this.totalKeys = totalKeys;
                return this;
            }

            public Builder totalRequests(long totalRequests) {
                this.totalRequests = totalRequests;
                return this;
            }

            public Builder totalHits(long totalHits) {
                this.totalHits = totalHits;
                return this;
            }

            public Builder totalMisses(long totalMisses) {
                this.totalMisses = totalMisses;
                return this;
            }

            public Builder overallHitRate(double overallHitRate) {
                this.overallHitRate = overallHitRate;
                return this;
            }

            public Builder keyMigrations(long keyMigrations) {
                this.keyMigrations = keyMigrations;
                return this;
            }

            public Builder serverStats(List<CacheServer.ServerStats> serverStats) {
                this.serverStats = new ArrayList<>(serverStats);
                return this;
            }

            public CacheStats build() {
                return new CacheStats(this);
            }
        }

        // Getters
        public int getTotalServers() {
            return totalServers;
        }

        public int getActiveServers() {
            return activeServers;
        }

        public int getTotalKeys() {
            return totalKeys;
        }

        public long getTotalRequests() {
            return totalRequests;
        }

        public long getTotalHits() {
            return totalHits;
        }

        public long getTotalMisses() {
            return totalMisses;
        }

        public double getOverallHitRate() {
            return overallHitRate;
        }

        public long getKeyMigrations() {
            return keyMigrations;
        }

        public List<CacheServer.ServerStats> getServerStats() {
            return new ArrayList<>(serverStats);
        }

        @Override
        public String toString() {
            return String.format(
                    "CacheStats{servers=%d/%d(active), keys=%d, requests=%d, hitRate=%.2f%%, migrations=%d}",
                    activeServers, totalServers, totalKeys, totalRequests, overallHitRate, keyMigrations);
        }
    }
}