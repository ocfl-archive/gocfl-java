package org.ocflarchive.gocfl.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.ocflarchive.gocfl.server.GocflServer;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Fluent builder for creating {@link GocflClient} instances.
 */
public class GocflClientBuilder {

    private String host = "127.0.0.1";
    private int port = 50051;
    private String target;
    private boolean plaintext = true;
    private GocflServer server;
    private LogHandler defaultLogHandler;
    private Consumer<ManagedChannelBuilder<?>> channelCustomizer;

    public GocflClientBuilder host(String host) {
        this.host = host;
        return this;
    }

    public GocflClientBuilder port(int port) {
        this.port = port;
        return this;
    }

    public GocflClientBuilder target(String target) {
        this.target = target;
        return this;
    }

    public GocflClientBuilder usePlaintext(boolean plaintext) {
        this.plaintext = plaintext;
        return this;
    }

    public GocflClientBuilder usePlaintext() {
        return usePlaintext(true);
    }

    public GocflClientBuilder useTransportSecurity() {
        return usePlaintext(false);
    }

    public GocflClientBuilder server(GocflServer server) {
        this.server = server;
        if (server != null) {
            this.host = server.getHost();
            this.port = server.getPort();
        }
        return this;
    }

    public GocflClientBuilder defaultLogHandler(LogHandler defaultLogHandler) {
        this.defaultLogHandler = defaultLogHandler;
        return this;
    }

    public GocflClientBuilder channelCustomizer(Consumer<ManagedChannelBuilder<?>> channelCustomizer) {
        this.channelCustomizer = channelCustomizer;
        return this;
    }

    public GocflClient build() {
        ManagedChannelBuilder<?> channelBuilder;
        if (target != null && !target.isBlank()) {
            channelBuilder = ManagedChannelBuilder.forTarget(target);
        } else {
            channelBuilder = ManagedChannelBuilder.forAddress(host, port);
        }

        if (plaintext) {
            channelBuilder.usePlaintext();
        }

        if (channelCustomizer != null) {
            channelCustomizer.accept(channelBuilder);
        }

        ManagedChannel channel = channelBuilder.build();
        return new GocflClient(channel, server, defaultLogHandler);
    }
}
