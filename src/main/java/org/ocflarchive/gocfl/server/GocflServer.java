package org.ocflarchive.gocfl.server;

import org.ocflarchive.gocfl.exception.GocflServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the lifecycle of a local gocfl-grpc server process.
 */
public class GocflServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GocflServer.class);
    private static final Pattern SERVER_STARTED_PATTERN = Pattern.compile("gocfl gRPC server started on\\s+([^\\s]+?)(?:\\.\\s|\\s*$)", Pattern.CASE_INSENSITIVE);

    private final GocflServerConfig config;
    private final Process process;
    private final String host;
    private final int port;
    private final Thread shutdownHook;
    private final ConcurrentLinkedQueue<String> recentOutput = new ConcurrentLinkedQueue<>();
    private volatile boolean closed = false;

    private GocflServer(GocflServerConfig config, Process process, String host, int port) {
        this.config = config;
        this.process = process;
        this.host = host;
        this.port = port;

        this.shutdownHook = new Thread(() -> {
            try {
                stopInternal();
            } catch (Exception ignored) {
            }
        }, "gocfl-server-shutdown-hook");

        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Starts a new gocfl-grpc server process using default configuration.
     */
    public static GocflServer start() {
        return start(GocflServerConfig.defaultConfig());
    }

    /**
     * Starts a new gocfl-grpc server process using the provided configuration.
     */
    public static GocflServer start(GocflServerConfig config) {
        Objects.requireNonNull(config, "config must not be null");

        Path executable = config.resolveExecutable();
        File exeFile = executable.toFile();
        if (!exeFile.exists() && !executable.toString().contains(File.separator)) {
            // Might be on PATH, allow system lookup
        } else if (!exeFile.exists()) {
            throw new GocflServerException("gocfl-grpc binary not found at: " + executable.toAbsolutePath());
        }

        List<String> command = new ArrayList<>();
        command.add(executable.toString());

        // Address flag
        String host = config.getHost();
        int requestedPort = config.getPort();
        command.add("-addr");
        command.add(host + ":" + requestedPort);

        if (config.isAllowApiShutdown()) {
            command.add("-allow-api-shutdown");
        }

        config.getConfigFile().ifPresent(cfg -> {
            command.add("-config");
            command.add(cfg.toString());
        });

        config.getLogFile().ifPresent(lf -> {
            command.add("-log-file");
            command.add(lf.toString());
        });

        config.getLogLevel().ifPresent(ll -> {
            command.add("-log-level");
            command.add(ll);
        });

        config.getS3Endpoint().ifPresent(ep -> {
            command.add("-s3-endpoint");
            command.add(ep);
        });

        config.getS3AccessKeyId().ifPresent(key -> {
            command.add("-s3-access-key-id");
            command.add(key);
        });

        config.getS3SecretAccessKey().ifPresent(secret -> {
            command.add("-s3-secret-access-key");
            command.add(secret);
        });

        config.getS3Region().ifPresent(region -> {
            command.add("-s3-region");
            command.add(region);
        });

        log.info("Starting gocfl-grpc server with command: {}", command);

        ProcessBuilder pb = new ProcessBuilder(command);
        if (config.isInheritIo()) {
            pb.inheritIO();
        }

        Process proc;
        try {
            proc = pb.start();
        } catch (IOException e) {
            throw new GocflServerException("Failed to launch gocfl-grpc process: " + e.getMessage(), e);
        }

        CompletableFuture<BoundAddress> addressFuture = new CompletableFuture<>();
        ConcurrentLinkedQueue<String> outputLines = new ConcurrentLinkedQueue<>();

        // Start stdout reader
        Thread stdoutThread = new Thread(() -> captureStream(proc.getInputStream(), false, addressFuture, outputLines), "gocfl-stdout-reader");
        stdoutThread.setDaemon(true);
        stdoutThread.start();

        // Start stderr reader
        Thread stderrThread = new Thread(() -> captureStream(proc.getErrorStream(), true, addressFuture, outputLines), "gocfl-stderr-reader");
        stderrThread.setDaemon(true);
        stderrThread.start();

        // Wait for server to become ready
        BoundAddress boundAddress;
        try {
            boundAddress = addressFuture.get(config.getStartupTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            proc.destroyForcibly();
            String output = String.join("\n", outputLines);
            throw new GocflServerException("Timed out waiting for gocfl-grpc server to start. Output:\n" + output, e);
        } catch (ExecutionException e) {
            proc.destroyForcibly();
            String output = String.join("\n", outputLines);
            throw new GocflServerException("Failed starting gocfl-grpc server: " + e.getCause().getMessage() + "\nOutput:\n" + output, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proc.destroyForcibly();
            throw new GocflServerException("Interrupted while waiting for gocfl-grpc server to start", e);
        }

        log.info("gocfl-grpc server successfully started on {}:{}", boundAddress.host, boundAddress.port);

        GocflServer server = new GocflServer(config, proc, boundAddress.host, boundAddress.port);
        server.recentOutput.addAll(outputLines);
        return server;
    }

    private static void captureStream(InputStream stream, boolean isError, CompletableFuture<BoundAddress> addressFuture, ConcurrentLinkedQueue<String> outputLines) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (outputLines.size() > 500) {
                    outputLines.poll();
                }
                outputLines.add(line);

                if (isError) {
                    log.warn("[gocfl-stderr] {}", line);
                } else {
                    log.debug("[gocfl-stdout] {}", line);
                }

                // Check for server started line
                Matcher matcher = SERVER_STARTED_PATTERN.matcher(line);
                if (matcher.find() && !addressFuture.isDone()) {
                    String rawAddr = matcher.group(1).trim();
                    BoundAddress addr = parseBoundAddress(rawAddr);
                    addressFuture.complete(addr);
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (!addressFuture.isDone()) {
                addressFuture.completeExceptionally(new GocflServerException("gocfl-grpc process stream ended unexpectedly before readiness"));
            }
        }
    }

    private static BoundAddress parseBoundAddress(String rawAddr) {
        // Examples: "127.0.0.1:50051", ":50051", "[::]:50051", "localhost:50051"
        try {
            String trimmed = rawAddr.trim();
            if (trimmed.endsWith(".")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            int lastColon = trimmed.lastIndexOf(':');
            if (lastColon >= 0) {
                String hostPart = trimmed.substring(0, lastColon).trim();
                String portPart = trimmed.substring(lastColon + 1).trim();
                int port = Integer.parseInt(portPart);

                if (hostPart.isEmpty() || hostPart.equals(":") || hostPart.equals("[::]") || hostPart.equals("0.0.0.0")) {
                    hostPart = "127.0.0.1";
                }
                return new BoundAddress(hostPart, port);
            }
        } catch (Exception e) {
            log.warn("Could not parse server address '{}', falling back to localhost:50051", rawAddr, e);
        }
        return new BoundAddress("127.0.0.1", 50051);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getTarget() {
        return host + ":" + port;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public Process getProcess() {
        return process;
    }

    public GocflServerConfig getConfig() {
        return config;
    }

    public List<String> getRecentOutput() {
        return new ArrayList<>(recentOutput);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM is already shutting down
        }
        stopInternal();
    }

    private void stopInternal() {
        if (!process.isAlive()) {
            return;
        }

        log.info("Stopping gocfl-grpc server process...");
        process.destroy();
        try {
            boolean exited = process.waitFor(config.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                log.warn("gocfl-grpc did not terminate gracefully within {} ms, forcing shutdown...", config.getShutdownTimeout().toMillis());
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        log.info("gocfl-grpc server process stopped.");
    }

    private static class BoundAddress {
        final String host;
        final int port;

        BoundAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
