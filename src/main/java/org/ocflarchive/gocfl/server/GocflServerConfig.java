package org.ocflarchive.gocfl.server;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration options for launching the gocfl-grpc server process.
 */
public class GocflServerConfig {

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 0; // Dynamic port
    public static final Duration DEFAULT_STARTUP_TIMEOUT = Duration.ofSeconds(15);
    public static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);

    private final Path executablePath;
    private final String host;
    private final int port;
    private final Path configFile;
    private final Path logFile;
    private final String logLevel;
    private final boolean allowApiShutdown;
    private final String s3Endpoint;
    private final String s3AccessKeyId;
    private final String s3SecretAccessKey;
    private final String s3Region;
    private final Duration startupTimeout;
    private final Duration shutdownTimeout;
    private final boolean inheritIo;

    private GocflServerConfig(Builder builder) {
        this.executablePath = builder.executablePath;
        this.host = builder.host != null ? builder.host : DEFAULT_HOST;
        this.port = builder.port;
        this.configFile = builder.configFile;
        this.logFile = builder.logFile;
        this.logLevel = builder.logLevel;
        this.allowApiShutdown = builder.allowApiShutdown;
        this.s3Endpoint = builder.s3Endpoint;
        this.s3AccessKeyId = builder.s3AccessKeyId;
        this.s3SecretAccessKey = builder.s3SecretAccessKey;
        this.s3Region = builder.s3Region;
        this.startupTimeout = builder.startupTimeout != null ? builder.startupTimeout : DEFAULT_STARTUP_TIMEOUT;
        this.shutdownTimeout = builder.shutdownTimeout != null ? builder.shutdownTimeout : DEFAULT_SHUTDOWN_TIMEOUT;
        this.inheritIo = builder.inheritIo;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static GocflServerConfig defaultConfig() {
        return builder().build();
    }

    public Optional<Path> getExecutablePath() {
        return Optional.ofNullable(executablePath);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Optional<Path> getConfigFile() {
        return Optional.ofNullable(configFile);
    }

    public Optional<Path> getLogFile() {
        return Optional.ofNullable(logFile);
    }

    public Optional<String> getLogLevel() {
        return Optional.ofNullable(logLevel);
    }

    public boolean isAllowApiShutdown() {
        return allowApiShutdown;
    }

    public Optional<String> getS3Endpoint() {
        return Optional.ofNullable(s3Endpoint);
    }

    public Optional<String> getS3AccessKeyId() {
        return Optional.ofNullable(s3AccessKeyId);
    }

    public Optional<String> getS3SecretAccessKey() {
        return Optional.ofNullable(s3SecretAccessKey);
    }

    public Optional<String> getS3Region() {
        return Optional.ofNullable(s3Region);
    }

    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public boolean isInheritIo() {
        return inheritIo;
    }

    /**
     * Resolves the gocfl-grpc binary executable path by checking:
     * 1. Explicitly configured path
     * 2. System property 'gocfl.grpc.path'
     * 3. Environment variable 'GOCFL_GRPC_PATH'
     * 4. Known common location (e.g. C:\daten\go\dev\gocfl-grpc\gocflgrpc.exe)
     * 5. Current directory or PATH
     */
    public Path resolveExecutable() {
        if (executablePath != null) {
            return executablePath;
        }

        String sysProp = System.getProperty("gocfl.grpc.path");
        if (sysProp != null && !sysProp.isBlank()) {
            return Paths.get(sysProp);
        }

        String envVar = System.getenv("GOCFL_GRPC_PATH");
        if (envVar != null && !envVar.isBlank()) {
            return Paths.get(envVar);
        }

        // Known default development path if present
        String isWindows = System.getProperty("os.name", "").toLowerCase();
        String exeName = isWindows.contains("win") ? "gocflgrpc.exe" : "gocflgrpc";

        Path knownDevPath = Paths.get("C:", "daten", "go", "dev", "gocfl-grpc", exeName);
        if (new File(knownDevPath.toString()).exists()) {
            return knownDevPath;
        }

        Path localPath = Paths.get(exeName);
        if (new File(localPath.toString()).exists()) {
            return localPath;
        }

        // Return exeName to be resolved via system PATH
        return Paths.get(exeName);
    }

    public static class Builder {
        private Path executablePath;
        private String host = DEFAULT_HOST;
        private int port = DEFAULT_PORT;
        private Path configFile;
        private Path logFile;
        private String logLevel;
        private boolean allowApiShutdown = true;
        private String s3Endpoint;
        private String s3AccessKeyId;
        private String s3SecretAccessKey;
        private String s3Region;
        private Duration startupTimeout = DEFAULT_STARTUP_TIMEOUT;
        private Duration shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;
        private boolean inheritIo = false;

        public Builder executablePath(Path executablePath) {
            this.executablePath = executablePath;
            return this;
        }

        public Builder executablePath(String executablePath) {
            this.executablePath = executablePath != null ? Paths.get(executablePath) : null;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder configFile(Path configFile) {
            this.configFile = configFile;
            return this;
        }

        public Builder configFile(String configFile) {
            this.configFile = configFile != null ? Paths.get(configFile) : null;
            return this;
        }

        public Builder logFile(Path logFile) {
            this.logFile = logFile;
            return this;
        }

        public Builder logFile(String logFile) {
            this.logFile = logFile != null ? Paths.get(logFile) : null;
            return this;
        }

        public Builder logLevel(String logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder allowApiShutdown(boolean allowApiShutdown) {
            this.allowApiShutdown = allowApiShutdown;
            return this;
        }

        public Builder s3Endpoint(String s3Endpoint) {
            this.s3Endpoint = s3Endpoint;
            return this;
        }

        public Builder s3AccessKeyId(String s3AccessKeyId) {
            this.s3AccessKeyId = s3AccessKeyId;
            return this;
        }

        public Builder s3SecretAccessKey(String s3SecretAccessKey) {
            this.s3SecretAccessKey = s3SecretAccessKey;
            return this;
        }

        public Builder s3Region(String s3Region) {
            this.s3Region = s3Region;
            return this;
        }

        public Builder startupTimeout(Duration startupTimeout) {
            this.startupTimeout = startupTimeout;
            return this;
        }

        public Builder shutdownTimeout(Duration shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        public Builder inheritIo(boolean inheritIo) {
            this.inheritIo = inheritIo;
            return this;
        }

        public GocflServerConfig build() {
            return new GocflServerConfig(this);
        }
    }
}
