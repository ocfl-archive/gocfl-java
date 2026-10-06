package org.ocflarchive.gocfl.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GocflServerConfigTest {

    @Test
    void testDefaultConfig() {
        GocflServerConfig config = GocflServerConfig.defaultConfig();

        assertThat(config.getHost()).isEqualTo("127.0.0.1");
        assertThat(config.getPort()).isEqualTo(0);
        assertThat(config.isAllowApiShutdown()).isTrue();
        assertThat(config.getStartupTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(config.getShutdownTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void testCustomBuilder() {
        Path exePath = Paths.get("custom", "path", "gocflgrpc.exe");
        GocflServerConfig config = GocflServerConfig.builder()
                .executablePath(exePath)
                .host("0.0.0.0")
                .port(50055)
                .logLevel("DEBUG")
                .allowApiShutdown(false)
                .startupTimeout(Duration.ofSeconds(30))
                .shutdownTimeout(Duration.ofSeconds(10))
                .build();

        assertThat(config.getExecutablePath()).contains(exePath);
        assertThat(config.getHost()).isEqualTo("0.0.0.0");
        assertThat(config.getPort()).isEqualTo(50055);
        assertThat(config.getLogLevel()).contains("DEBUG");
        assertThat(config.isAllowApiShutdown()).isFalse();
        assertThat(config.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getShutdownTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void testResolveExecutableFallback() {
        GocflServerConfig config = GocflServerConfig.defaultConfig();
        Path resolved = config.resolveExecutable();
        assertThat(resolved).isNotNull();
    }
}
