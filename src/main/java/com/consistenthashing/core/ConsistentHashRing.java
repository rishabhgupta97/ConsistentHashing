package com.consistenthashing.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A thread-safe implementation of Consistent Hashing algorithm.
 * 
 * This class implements the consistent hashing algorithm which ensures that
 * when
 * servers are added or removed, only a minimum number of keys need to be
 * remapped.
 * It also uses virtual nodes (replicas) to ensure better load distribution.
 * 
 * Key Features:
 * - Thread-safe operations using ReadWriteLock
 * - Virtual nodes for better load distribution
 * - Configurable number of virtual nodes per server
 * - SHA-256 hashing for uniform distribution
 * - Efficient O(log n) lookup using TreeMap
 * 
 * @author Your Name
 * @version 1.0
 */
public class ConsistentHashRing<T> {

    // TreeMap to store the hash ring with hash values as keys and servers as values
    private final TreeMap<Long, T> ring = new TreeMap<>();

    // Map to track virtual nodes for each server
    private final Map<T, Set<Long>> serverVirtualNodes = new ConcurrentHashMap<>();

    // Number of virtual nodes per server (default: 150 for good distribution)
    private final int virtualNodes;

    // ReadWriteLock for thread-safe operations
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // MessageDigest for hashing (SHA-256)
    private final MessageDigest md;

    /**
     * Default constructor with 150 virtual nodes per server
     */
    public ConsistentHashRing() {
        this(150);
    }

    /**
     * Constructor with custom number of virtual nodes
     * 
     * @param virtualNodes Number of virtual nodes per server
     * @throws IllegalArgumentException if virtualNodes <= 0
     */
    public ConsistentHashRing(int virtualNodes) {
        if (virtualNodes <= 0) {
            throw new IllegalArgumentException("Virtual nodes must be positive");
        }
        this.virtualNodes = virtualNodes;
        try {
            this.md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Adds a server to the hash ring
     * 
     * @param server The server to add
     * @throws IllegalArgumentException if server is null
     */
    public void addServer(T server) {
        if (server == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }

        lock.writeLock().lock();
        try {
            Set<Long> virtualNodeHashes = new HashSet<>();

            // Add virtual nodes for the server
            for (int i = 0; i < virtualNodes; i++) {
                String virtualNodeKey = server.toString() + ":" + i;
                long hash = hash(virtualNodeKey);
                ring.put(hash, server);
                virtualNodeHashes.add(hash);
            }

            serverVirtualNodes.put(server, virtualNodeHashes);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a server from the hash ring
     * 
     * @param server The server to remove
     * @return true if server was removed, false if server was not present
     */
    public boolean removeServer(T server) {
        if (server == null) {
            return false;
        }

        lock.writeLock().lock();
        try {
            Set<Long> virtualNodeHashes = serverVirtualNodes.get(server);
            if (virtualNodeHashes == null) {
                return false;
            }

            // Remove all virtual nodes for this server
            for (Long hash : virtualNodeHashes) {
                ring.remove(hash);
            }

            serverVirtualNodes.remove(server);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the server responsible for a given key
     * 
     * @param key The key to lookup
     * @return The server responsible for the key, null if no servers available
     * @throws IllegalArgumentException if key is null
     */
    public T getServer(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        lock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                return null;
            }

            long hash = hash(key);

            // Find the first server in clockwise direction
            Map.Entry<Long, T> entry = ring.ceilingEntry(hash);

            // If no server found after the hash, wrap around to the first server
            if (entry == null) {
                entry = ring.firstEntry();
            }

            return entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets all servers currently in the ring
     * 
     * @return Set of all servers
     */
    public Set<T> getServers() {
        lock.readLock().lock();
        try {
            return new HashSet<>(serverVirtualNodes.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the number of servers in the ring
     * 
     * @return Number of servers
     */
    public int getServerCount() {
        lock.readLock().lock();
        try {
            return serverVirtualNodes.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the total number of virtual nodes in the ring
     * 
     * @return Total number of virtual nodes
     */
    public int getTotalVirtualNodes() {
        lock.readLock().lock();
        try {
            return ring.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets distribution statistics for servers
     * 
     * @param keys List of keys to analyze distribution for
     * @return Map of server to number of keys assigned to it
     */
    public Map<T, Integer> getDistributionStats(List<String> keys) {
        if (keys == null) {
            throw new IllegalArgumentException("Keys list cannot be null");
        }

        Map<T, Integer> stats = new HashMap<>();

        lock.readLock().lock();
        try {
            // Initialize stats for all servers
            for (T server : serverVirtualNodes.keySet()) {
                stats.put(server, 0);
            }

            // Count keys per server
            for (String key : keys) {
                T server = getServer(key);
                if (server != null) {
                    stats.put(server, stats.get(server) + 1);
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return stats;
    }

    /**
     * Calculates the hash of a string using SHA-256
     * 
     * @param input The string to hash
     * @return The hash value as a long
     */
    private long hash(String input) {
        synchronized (md) {
            md.reset();
            md.update(input.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();

            // Convert first 8 bytes to long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        }
    }

    /**
     * Checks if the ring is empty
     * 
     * @return true if ring has no servers
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return ring.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears all servers from the ring
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            ring.clear();
            serverVirtualNodes.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("ConsistentHashRing{servers=%d, virtualNodes=%d, totalVirtualNodes=%d}",
                    serverVirtualNodes.size(), virtualNodes, ring.size());
        } finally {
            lock.readLock().unlock();
        }
    }
}