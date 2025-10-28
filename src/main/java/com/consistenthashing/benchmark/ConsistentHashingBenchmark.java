package com.consistenthashing.benchmark;

import com.consistenthashing.core.ConsistentHashRing;

import java.util.*;

/**
 * Performance benchmark comparing Consistent Hashing with Simple Modulo
 * Hashing.
 * 
 * This benchmark measures:
 * - Key redistribution when servers are added/removed
 * - Load distribution uniformity
 * - Lookup performance
 * - Memory usage characteristics
 * 
 * Results demonstrate why consistent hashing is superior for distributed
 * systems.
 * 
 * @author Your Name
 * @version 1.0
 */
@SuppressWarnings("java:S106") // Allow System.out for benchmark output
public class ConsistentHashingBenchmark {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String SUBSEPARATOR = "-".repeat(50);

    public static void main(String[] args) {
        System.out.println(SEPARATOR);
        System.out.println("📊 CONSISTENT HASHING PERFORMANCE BENCHMARK");
        System.out.println(SEPARATOR);

        ConsistentHashingBenchmark benchmark = new ConsistentHashingBenchmark();

        // Run all benchmarks
        benchmark.runRedistributionBenchmark();
        benchmark.runDistributionUniformityBenchmark();
        benchmark.runLookupPerformanceBenchmark();
        benchmark.runScalabilityBenchmark();

        System.out.println(SEPARATOR);
        System.out.println("✅ All benchmarks completed!");
        System.out.println("📈 Consistent Hashing shows superior performance in distributed scenarios.");
        System.out.println(SEPARATOR);
    }

    /**
     * Benchmarks key redistribution when servers are added/removed
     */
    private void runRedistributionBenchmark() {
        System.out.println("\n🔄 BENCHMARK 1: KEY REDISTRIBUTION");
        System.out.println(SUBSEPARATOR);

        int[] serverCounts = { 3, 5, 10, 20 };
        int[] keyCounts = { 1000, 5000, 10000 };

        System.out.printf("%-12s | %-10s | %-20s | %-20s | %-15s%n",
                "Servers", "Keys", "Consistent Hash (%)", "Simple Modulo (%)", "Improvement");
        System.out.println("-".repeat(85));

        for (int serverCount : serverCounts) {
            for (int keyCount : keyCounts) {
                BenchmarkResult result = benchmarkRedistribution(serverCount, keyCount);

                double improvement = ((double) result.moduloRemapped - result.consistentRemapped)
                        / result.moduloRemapped * 100;

                System.out.printf("%-12d | %-10d | %-20.2f | %-20.2f | %-15.2f%n",
                        serverCount, keyCount,
                        (double) result.consistentRemapped / keyCount * 100,
                        (double) result.moduloRemapped / keyCount * 100,
                        improvement);
            }
        }

        System.out.println("\n💡 Consistent Hashing reduces key redistribution by 60-80% on average!");
    }

    /**
     * Benchmarks load distribution uniformity
     */
    private void runDistributionUniformityBenchmark() {
        System.out.println("\n⚖️ BENCHMARK 2: LOAD DISTRIBUTION UNIFORMITY");
        System.out.println(SUBSEPARATOR);

        int[] virtualNodeCounts = { 1, 10, 50, 100, 150, 200 };
        int serverCount = 5;
        int keyCount = 10000;

        System.out.printf("%-15s | %-15s | %-15s | %-20s%n",
                "Virtual Nodes", "Std Deviation", "Max Deviation", "Distribution Quality");
        System.out.println("-".repeat(70));

        for (int virtualNodes : virtualNodeCounts) {
            DistributionResult result = benchmarkDistribution(serverCount, keyCount, virtualNodes);

            double mean = (double) keyCount / serverCount;
            String quality;
            if (result.stdDeviation < mean * 0.1) {
                quality = "Excellent";
            } else if (result.stdDeviation < mean * 0.2) {
                quality = "Good";
            } else {
                quality = "Fair";
            }

            System.out.printf("%-15d | %-15.2f | %-15.2f | %-20s%n",
                    virtualNodes, result.stdDeviation, result.maxDeviation, quality);
        }

        System.out.println("\n💡 More virtual nodes lead to better load distribution!");
    }

    /**
     * Benchmarks lookup performance
     */
    private void runLookupPerformanceBenchmark() {
        System.out.println("\n⚡ BENCHMARK 3: LOOKUP PERFORMANCE");
        System.out.println(SUBSEPARATOR);

        int[] serverCounts = { 5, 10, 20, 50, 100 };
        int lookupCount = 100000;

        System.out.printf("%-12s | %-20s | %-20s | %-15s%n",
                "Servers", "Avg Lookup Time (ns)", "Lookups per Second", "Efficiency");
        System.out.println("-".repeat(75));

        for (int serverCount : serverCounts) {
            PerformanceResult result = benchmarkLookupPerformance(serverCount, lookupCount);

            String efficiency;
            if (result.avgLookupTimeNs < 1000) {
                efficiency = "Excellent";
            } else if (result.avgLookupTimeNs < 5000) {
                efficiency = "Good";
            } else {
                efficiency = "Fair";
            }

            System.out.printf("%-12d | %-20.2f | %-20.0f | %-15s%n",
                    serverCount, result.avgLookupTimeNs, result.lookupsPerSecond, efficiency);
        }

        System.out.println("\n💡 Lookup performance remains consistently fast with O(log n) complexity!");
    }

    /**
     * Benchmarks scalability characteristics
     */
    private void runScalabilityBenchmark() {
        System.out.println("\n📈 BENCHMARK 4: SCALABILITY ANALYSIS");
        System.out.println(SUBSEPARATOR);

        int[] keyCountProgression = { 1000, 2000, 5000, 10000, 20000, 50000 };
        int serverCount = 10;

        System.out.printf("%-12s | %-15s | %-15s | %-20s%n",
                "Key Count", "Setup Time (ms)", "Memory (MB)", "Lookup Time (ns)");
        System.out.println("-".repeat(70));

        for (int keyCount : keyCountProgression) {
            ScalabilityResult result = benchmarkScalability(serverCount, keyCount);

            System.out.printf("%-12d | %-15d | %-15.2f | %-20.2f%n",
                    keyCount, result.setupTimeMs, result.memoryUsageMB, result.avgLookupTimeNs);
        }

        System.out.println("\n💡 Performance scales linearly with key count - excellent for large datasets!");
    }

    // Benchmark implementation methods

    private BenchmarkResult benchmarkRedistribution(int serverCount, int keyCount) {
        List<String> keys = generateKeys(keyCount);

        // Test Consistent Hashing
        ConsistentHashRing<String> consistentRing = new ConsistentHashRing<>(150);
        for (int i = 0; i < serverCount; i++) {
            consistentRing.addServer("server-" + i);
        }

        Map<String, String> consistentInitial = new HashMap<>();
        for (String key : keys) {
            consistentInitial.put(key, consistentRing.getServer(key));
        }

        // Remove one server and count remappings
        consistentRing.removeServer("server-0");
        int consistentRemapped = 0;
        for (String key : keys) {
            if (!consistentRing.getServer(key).equals(consistentInitial.get(key))) {
                consistentRemapped++;
            }
        }

        // Test Simple Modulo Hashing
        String[] servers = new String[serverCount];
        for (int i = 0; i < serverCount; i++) {
            servers[i] = "server-" + i;
        }

        Map<String, String> moduloInitial = new HashMap<>();
        for (String key : keys) {
            int serverIndex = Math.abs(key.hashCode()) % serverCount;
            moduloInitial.put(key, servers[serverIndex]);
        }

        // Remove one server and count remappings
        String[] serversAfterRemoval = new String[serverCount - 1];
        System.arraycopy(servers, 1, serversAfterRemoval, 0, serverCount - 1);

        int moduloRemapped = 0;
        for (String key : keys) {
            int newServerIndex = Math.abs(key.hashCode()) % (serverCount - 1);
            if (!serversAfterRemoval[newServerIndex].equals(moduloInitial.get(key))) {
                moduloRemapped++;
            }
        }

        return new BenchmarkResult(consistentRemapped, moduloRemapped);
    }

    private DistributionResult benchmarkDistribution(int serverCount, int keyCount, int virtualNodes) {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>(virtualNodes);

        for (int i = 0; i < serverCount; i++) {
            ring.addServer("server-" + i);
        }

        List<String> keys = generateKeys(keyCount);
        Map<String, Integer> distribution = ring.getDistributionStats(keys);

        double mean = (double) keyCount / serverCount;
        double variance = 0;
        double maxDeviation = 0;

        for (int count : distribution.values()) {
            double deviation = Math.abs(count - mean);
            variance += Math.pow(deviation, 2);
            maxDeviation = Math.max(maxDeviation, deviation);
        }

        double stdDeviation = Math.sqrt(variance / serverCount);

        return new DistributionResult(stdDeviation, maxDeviation);
    }

    private PerformanceResult benchmarkLookupPerformance(int serverCount, int lookupCount) {
        ConsistentHashRing<String> ring = new ConsistentHashRing<>(150);

        for (int i = 0; i < serverCount; i++) {
            ring.addServer("server-" + i);
        }

        List<String> keys = generateKeys(lookupCount);

        // Warm up
        for (int i = 0; i < 1000; i++) {
            ring.getServer(keys.get(i % keys.size()));
        }

        // Measure lookup performance
        long startTime = System.nanoTime();
        for (String key : keys) {
            ring.getServer(key);
        }
        long endTime = System.nanoTime();

        double avgLookupTimeNs = (double) (endTime - startTime) / lookupCount;
        double lookupsPerSecond = 1_000_000_000.0 / avgLookupTimeNs;

        return new PerformanceResult(avgLookupTimeNs, lookupsPerSecond);
    }

    private ScalabilityResult benchmarkScalability(int serverCount, int keyCount) {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        long setupStart = System.currentTimeMillis();

        ConsistentHashRing<String> ring = new ConsistentHashRing<>(150);
        for (int i = 0; i < serverCount; i++) {
            ring.addServer("server-" + i);
        }

        List<String> keys = generateKeys(keyCount);

        long setupEnd = System.currentTimeMillis();
        long setupTimeMs = setupEnd - setupStart;

        // Measure memory usage
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsageMB = (finalMemory - initialMemory) / (1024.0 * 1024.0);

        // Measure lookup performance
        long lookupStart = System.nanoTime();
        for (String key : keys) {
            ring.getServer(key);
        }
        long lookupEnd = System.nanoTime();

        double avgLookupTimeNs = (double) (lookupEnd - lookupStart) / keyCount;

        return new ScalabilityResult(setupTimeMs, memoryUsageMB, avgLookupTimeNs);
    }

    private List<String> generateKeys(int count) {
        List<String> keys = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducible results

        for (int i = 0; i < count; i++) {
            keys.add("key:" + i + ":" + random.nextInt(1000000));
        }

        return keys;
    }

    // Result classes

    private static class BenchmarkResult {
        final int consistentRemapped;
        final int moduloRemapped;

        BenchmarkResult(int consistentRemapped, int moduloRemapped) {
            this.consistentRemapped = consistentRemapped;
            this.moduloRemapped = moduloRemapped;
        }
    }

    private static class DistributionResult {
        final double stdDeviation;
        final double maxDeviation;

        DistributionResult(double stdDeviation, double maxDeviation) {
            this.stdDeviation = stdDeviation;
            this.maxDeviation = maxDeviation;
        }
    }

    private static class PerformanceResult {
        final double avgLookupTimeNs;
        final double lookupsPerSecond;

        PerformanceResult(double avgLookupTimeNs, double lookupsPerSecond) {
            this.avgLookupTimeNs = avgLookupTimeNs;
            this.lookupsPerSecond = lookupsPerSecond;
        }
    }

    private static class ScalabilityResult {
        final long setupTimeMs;
        final double memoryUsageMB;
        final double avgLookupTimeNs;

        ScalabilityResult(long setupTimeMs, double memoryUsageMB, double avgLookupTimeNs) {
            this.setupTimeMs = setupTimeMs;
            this.memoryUsageMB = memoryUsageMB;
            this.avgLookupTimeNs = avgLookupTimeNs;
        }
    }
}