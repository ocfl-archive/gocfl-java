package org.ocflarchive.gocfl;

import org.ocflarchive.gocfl.client.GocflClient;
import org.ocflarchive.gocfl.client.GocflClientBuilder;
import org.ocflarchive.gocfl.proto.InitRequest;
import org.ocflarchive.gocfl.proto.User;
import org.ocflarchive.gocfl.proto.ValidateRequest;
import org.ocflarchive.gocfl.server.GocflServer;
import org.ocflarchive.gocfl.server.GocflServerConfig;

/**
 * Main entry point and convenience factory for gocfl-java.
 */
public final class Gocfl {

    private Gocfl() {
    }

    /**
     * Creates a new client builder for configuring and creating {@link GocflClient} instances.
     */
    public static GocflClientBuilder clientBuilder() {
        return GocflClient.builder();
    }

    /**
     * Connects to a running gocfl-grpc service at the given host and port.
     */
    public static GocflClient connect(String host, int port) {
        return GocflClient.connect(host, port);
    }

    /**
     * Connects to a running gocfl-grpc service at the specified target (e.g. "localhost:50051").
     */
    public static GocflClient connect(String target) {
        return GocflClient.connect(target);
    }

    /**
     * Launches a local gocfl-grpc server process with default settings (dynamic port allocation)
     * and returns a connected {@link GocflClient}.
     * Closing the returned client will automatically stop the server process.
     */
    public static GocflClient startServer() {
        return GocflClient.startServer();
    }

    /**
     * Launches a local gocfl-grpc server process with the given configuration and returns a connected {@link GocflClient}.
     * Closing the returned client will automatically stop the server process.
     */
    public static GocflClient startServer(GocflServerConfig config) {
        return GocflClient.startServer(config);
    }

    /**
     * Launches a standalone {@link GocflServer} instance.
     */
    public static GocflServer launchServer(GocflServerConfig config) {
        return GocflServer.start(config);
    }

    // --- Helper Factory Methods for Protobuf Messages ---

    /**
     * Creates a new OCFL {@link User} object with the given name and address.
     */
    public static User user(String name, String address) {
        return User.newBuilder()
                .setName(name)
                .setAddress(address)
                .build();
    }

    /**
     * Creates an {@link InitRequest} for the given OCFL storage root path.
     */
    public static InitRequest initRequest(String ocflPath) {
        return InitRequest.newBuilder()
                .setOcflPath(ocflPath)
                .build();
    }

    /**
     * Creates a {@link ValidateRequest} for the given OCFL storage root path.
     */
    public static ValidateRequest validateRequest(String ocflPath) {
        return ValidateRequest.newBuilder()
                .setOcflPath(ocflPath)
                .build();
    }
}
