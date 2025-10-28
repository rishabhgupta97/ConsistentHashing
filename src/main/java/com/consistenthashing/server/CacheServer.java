package com.consistenthashing.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;

/**
 * Simulates a cache server with key-value storage and metrics tracking.
 * 
 * This class represents a cache server node in a distributed caching system.
 * It provides basic cache operations (get, put, remove) and tracks important
 * metrics like hit rate, request count, and data size.
 * 
 * Features:
 * - Thread-safe operations using ConcurrentHashMap
 * - Metrics tracking (hits, misses, request count)
 * - Memory usage simulation
 * - Server health status
 * 
 * @author Your Name
 * @version 1.0
 */
public class CacheServer {

    private final String serverId;
    private final ConcurrentHashMap<String, Object> cache;

    // Metrics
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicInteger dataSize = new AtomicInteger(0);

    // Server status
    private volatile boolean isActive = true;
    private final long startTime;

    /**
     * Creates a new cache server with the given ID
     * 
     * @param serverId Unique identifier for this server
     * @throws IllegalArgumentException if serverId is null or empty
     */
    public CacheServer(String serverId) {
        if (serverId == null || serverId.trim().isEmpty()) {
            throw new IllegalArgumentException("Server ID cannot be null or empty");
        }
        this.serverId = serverId.trim();
        this.cache = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Retrieves a value from the cache
     * 
     * @param key The key to lookup
     * @return The value associated with the key, null if not found
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException    if server is not active
     */
    public Object get(String key) {
        validateKey(key);
        checkServerActive();

        requestCount.incrementAndGet();
        Object value = cache.get(key);

        if (value != null) {
            hitCount.incrementAndGet();
        } else {
            missCount.incrementAndGet();
        }

        return value;
    }

    /**
     * Stores a key-value pair in the cache
     * 
     * @param key   The key to store
     * @param value The value to associate with the key
     * @return The previous value associated with the key, null if none
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException    if server is not active
     */
    public Object put(String key, Object value) {
        validateKey(key);
        checkServerActive();

        Object previousValue = cache.put(key, value);

        if (previousValue == null) {
            dataSize.incrementAndGet();
        }

        return previousValue;
    }

    /**
     * Removes a key-value pair from the cache
     * 
     * @param key The key to remove
     * @return The value that was associated with the key, null if none
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException    if server is not active
     */
    public Object remove(String key) {
        validateKey(key);
        checkServerActive();

        Object removedValue = cache.remove(key);

        if (removedValue != null) {
            dataSize.decrementAndGet();
        }

        return removedValue;
    }

    /**
     * Checks if a key exists in the cache
     * 
     * @param key The key to check
     * @return true if key exists, false otherwise
     * @throws IllegalArgumentException if key is null
     * @throws IllegalStateException    if server is not active
     */
    public boolean containsKey(String key) {
        validateKey(key);
        checkServerActive();

        return cache.containsKey(key);
    }

    /**
     * Gets all keys stored in this cache server
     * 
     * @return Set of all keys
     * @throws IllegalStateException if server is not active
     */
    public Set<String> getKeys() {
        checkServerActive();
        return cache.keySet();
    }

    /**
     * Gets the number of items in the cache
     * 
     * @return Number of cached items
     */
    public int size() {
        return cache.size();
    }

    /**
     * Clears all data from the cache
     * 
     * @throws IllegalStateException if server is not active
     */
    public void clear() {
        checkServerActive();
        cache.clear();
        dataSize.set(0);
    }

    /**
     * Gets the server ID
     * 
     * @return The server ID
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Gets the cache hit rate
     * 
     * @return Hit rate as a percentage (0-100)
     */
    public double getHitRate() {
        long total = hitCount.get() + missCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) hitCount.get() / total * 100.0;
    }

    /**
     * Gets the total number of cache hits
     * 
     * @return Number of cache hits
     */
    public long getHitCount() {
        return hitCount.get();
    }

    /**
     * Gets the total number of cache misses
     * 
     * @return Number of cache misses
     */
    public long getMissCount() {
        return missCount.get();
    }

    /**
     * Gets the total number of requests served
     * 
     * @return Total request count
     */
    public long getRequestCount() {
        return requestCount.get();
    }

    /**
     * Gets the number of unique keys stored
     * 
     * @return Number of stored keys
     */
    public int getDataSize() {
        return dataSize.get();
    }

    /**
     * Gets the server uptime in milliseconds
     * 
     * @return Uptime in milliseconds
     */
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Checks if the server is active
     * 
     * @return true if server is active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets the server active status
     * 
     * @param active true to activate server, false to deactivate
     */
    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Simulates server failure by deactivating it
     */
    public void simulateFailure() {
        this.isActive = false;
    }

    /**
     * Simulates server recovery by reactivating it
     */
    public void recover() {
        this.isActive = true;
    }

    /**
     * Gets comprehensive server statistics
     * 
     * @return ServerStats object containing all metrics
     */
    public ServerStats getStats() {
        return ServerStats.builder(serverId)
                .active(isActive)
                .hitRate(getHitRate())
                .hitCount(hitCount.get())
                .missCount(missCount.get())
                .requestCount(requestCount.get())
                .dataSize(dataSize.get())
                .uptime(getUptime())
                .build();
    }

    /**
     * Resets all metrics (useful for testing)
     */
    public void resetMetrics() {
        hitCount.set(0);
        missCount.set(0);
        requestCount.set(0);
    }

    private void validateKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
    }

    private void checkServerActive() {
        if (!isActive) {
            throw new IllegalStateException("Server " + serverId + " is not active");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CacheServer that = (CacheServer) obj;
        return serverId.equals(that.serverId);
    }

    @Override
    public int hashCode() {
        return serverId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("CacheServer{id='%s', active=%s, size=%d, hitRate=%.2f%%}",
                serverId, isActive, dataSize.get(), getHitRate());
    }

    /**
     * Immutable class representing server statistics
     */
    public static class ServerStats {
        private final String serverId;
        private final boolean isActive;
        private final double hitRate;
        private final long hitCount;
        private final long missCount;
        private final long requestCount;
        private final int dataSize;
        private final long uptime;

        private ServerStats(Builder builder) {
            this.serverId = builder.serverId;
            this.isActive = builder.isActive;
            this.hitRate = builder.hitRate;
            this.hitCount = builder.hitCount;
            this.missCount = builder.missCount;
            this.requestCount = builder.requestCount;
            this.dataSize = builder.dataSize;
            this.uptime = builder.uptime;
        }

        public static Builder builder(String serverId) {
            return new Builder(serverId);
        }

        public static class Builder {
            private final String serverId;
            private boolean isActive;
            private double hitRate;
            private long hitCount;
            private long missCount;
            private long requestCount;
            private int dataSize;
            private long uptime;

            private Builder(String serverId) {
                this.serverId = serverId;
            }

            public Builder active(boolean isActive) {
                this.isActive = isActive;
                return this;
            }

            public Builder hitRate(double hitRate) {
                this.hitRate = hitRate;
                return this;
            }

            public Builder hitCount(long hitCount) {
                this.hitCount = hitCount;
                return this;
            }

            public Builder missCount(long missCount) {
                this.missCount = missCount;
                return this;
            }

            public Builder requestCount(long requestCount) {
                this.requestCount = requestCount;
                return this;
            }

            public Builder dataSize(int dataSize) {
                this.dataSize = dataSize;
                return this;
            }

            public Builder uptime(long uptime) {
                this.uptime = uptime;
                return this;
            }

            public ServerStats build() {
                return new ServerStats(this);
            }
        }

        // Getters
        public String getServerId() {
            return serverId;
        }

        public boolean isActive() {
            return isActive;
        }

        public double getHitRate() {
            return hitRate;
        }

        public long getHitCount() {
            return hitCount;
        }

        public long getMissCount() {
            return missCount;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public int getDataSize() {
            return dataSize;
        }

        public long getUptime() {
            return uptime;
        }

        @Override
        public String toString() {
            return String.format(
                    "ServerStats{id='%s', active=%s, hitRate=%.2f%%, requests=%d, dataSize=%d, uptime=%dms}",
                    serverId, isActive, hitRate, requestCount, dataSize, uptime);
        }
    }
}