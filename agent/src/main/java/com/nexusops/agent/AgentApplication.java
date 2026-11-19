package com.nexusops.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentApplication {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) {
        String serverUrl = required("NEXUSOPS_SERVER_URL");
        String agentId = required("NEXUSOPS_AGENT_ID");
        String credential = required("NEXUSOPS_AGENT_CREDENTIAL");
        long intervalSeconds = Long.parseLong(System.getenv().getOrDefault("NEXUSOPS_COLLECTION_INTERVAL_SECONDS", "30"));
        if (intervalSeconds < 5) {
            throw new IllegalArgumentException("NEXUSOPS_COLLECTION_INTERVAL_SECONDS must be at least 5");
        }

        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        long[][] previousTicks = new long[][] {processor.getSystemCpuLoadTicks()};
        HttpClient httpClient = HttpClient.newHttpClient();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable collection = () -> {
            try {
                List<Map<String, Object>> samples = collect(systemInfo, hardware, processor, previousTicks[0]);
                previousTicks[0] = processor.getSystemCpuLoadTicks();

                heartbeat(httpClient, serverUrl, agentId, credential);
                ingest(httpClient, serverUrl, agentId, credential, samples);
                System.out.println("NexusOps agent sent " + samples.size() + " metric samples");
            } catch (Exception exception) {
                System.err.println("Metric collection failed: " + exception.getMessage());
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdownNow));
        collection.run();
        scheduler.scheduleAtFixedRate(collection, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private static List<Map<String, Object>> collect(
            SystemInfo systemInfo,
            HardwareAbstractionLayer hardware,
            CentralProcessor processor,
            long[] previousTicks
    ) {
        Instant recordedAt = Instant.now();
        double cpuUsage = processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100.0;
        if (!Double.isFinite(cpuUsage)) {
            cpuUsage = 0.0;
        }
        GlobalMemory memory = hardware.getMemory();
        long memoryUsed = memory.getTotal() - memory.getAvailable();
        double memoryUsage = memory.getTotal() == 0
                ? 0.0 : memoryUsed * 100.0 / memory.getTotal();

        List<Map<String, Object>> samples = new ArrayList<>();
        samples.add(sample("cpu", "cpu.usage_percent", round(cpuUsage), "percent", recordedAt));
        samples.add(sample("memory", "memory.used_bytes", memoryUsed, "bytes", recordedAt));
        samples.add(sample("memory", "memory.usage_percent", round(memoryUsage), "percent", recordedAt));
        samples.add(sample("process", "process.count", systemInfo.getOperatingSystem().getProcessCount(), "count", recordedAt));

        long diskUsed = 0;
        long diskTotal = 0;
        for (var store : systemInfo.getOperatingSystem().getFileSystem().getFileStores()) {
            long usable = store.getUsableSpace();
            long total = store.getTotalSpace();
            diskUsed += total - usable;
            diskTotal += total;
        }
        double diskUsage = diskTotal == 0 ? 0.0 : diskUsed * 100.0 / diskTotal;
        samples.add(sample("disk", "disk.used_bytes", diskUsed, "bytes", recordedAt));
        samples.add(sample("disk", "disk.usage_percent", round(diskUsage), "percent", recordedAt));

        long rxBytes = 0;
        long txBytes = 0;
        for (NetworkIF network : hardware.getNetworkIFs()) {
            rxBytes += network.getBytesRecv();
            txBytes += network.getBytesSent();
        }
        samples.add(sample("network", "network.received_bytes", rxBytes, "bytes", recordedAt));
        samples.add(sample("network", "network.sent_bytes", txBytes, "bytes", recordedAt));
        return samples;
    }

    private static Map<String, Object> sample(
            String metricType,
            String metricName,
            Number value,
            String unit,
            Instant recordedAt
    ) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("metricType", metricType);
        sample.put("metricName", metricName);
        sample.put("value", value);
        sample.put("unit", unit);
        sample.put("labels", Map.of());
        sample.put("recordedAt", recordedAt.toString());
        return sample;
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void heartbeat(
            HttpClient client,
            String serverUrl,
            String agentId,
            String credential
    ) throws Exception {
        send(client, serverUrl + "/api/v1/agent/heartbeat", agentId, credential, "{}");
    }

    private static void ingest(
            HttpClient client,
            String serverUrl,
            String agentId,
            String credential,
            List<Map<String, Object>> samples
    ) throws Exception {
        String body = JSON.writeValueAsString(Map.of("samples", samples));
        send(client, serverUrl + "/api/v1/agent/metrics", agentId, credential, body);
    }

    private static void send(
            HttpClient client,
            String url,
            String agentId,
            String credential,
            String body
    ) throws Exception {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .header("Content-Type", "application/json")
                        .header("X-Agent-Id", agentId)
                        .header("X-Agent-Credential", credential)
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return;
                }
                lastFailure = new IllegalStateException(
                        "NexusOps returned HTTP " + response.statusCode() + ": " + response.body()
                );
            } catch (Exception exception) {
                lastFailure = exception;
            }
            if (attempt < 3) {
                Thread.sleep(1000L * attempt);
            }
        }
        throw lastFailure == null ? new IllegalStateException("Agent request failed") : lastFailure;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
