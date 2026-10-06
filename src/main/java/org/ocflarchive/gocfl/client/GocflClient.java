package org.ocflarchive.gocfl.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.ocflarchive.gocfl.exception.GocflException;
import org.ocflarchive.gocfl.exception.GocflOperationException;
import org.ocflarchive.gocfl.exception.GocflValidationException;
import org.ocflarchive.gocfl.proto.AddRequest;
import org.ocflarchive.gocfl.proto.AddResponse;
import org.ocflarchive.gocfl.proto.AddResult;
import org.ocflarchive.gocfl.proto.CreateRequest;
import org.ocflarchive.gocfl.proto.CreateResponse;
import org.ocflarchive.gocfl.proto.CreateResult;
import org.ocflarchive.gocfl.proto.GocflServiceGrpc;
import org.ocflarchive.gocfl.proto.GocflServiceGrpc.GocflServiceBlockingStub;
import org.ocflarchive.gocfl.proto.GocflServiceGrpc.GocflServiceFutureStub;
import org.ocflarchive.gocfl.proto.GocflServiceGrpc.GocflServiceStub;
import org.ocflarchive.gocfl.proto.InitRequest;
import org.ocflarchive.gocfl.proto.InitResponse;
import org.ocflarchive.gocfl.proto.InitResult;
import org.ocflarchive.gocfl.proto.ShutdownRequest;
import org.ocflarchive.gocfl.proto.ShutdownResponse;
import org.ocflarchive.gocfl.proto.UpdateRequest;
import org.ocflarchive.gocfl.proto.UpdateResponse;
import org.ocflarchive.gocfl.proto.UpdateResult;
import org.ocflarchive.gocfl.proto.ValidateRequest;
import org.ocflarchive.gocfl.proto.ValidateResponse;
import org.ocflarchive.gocfl.proto.ValidateResult;
import org.ocflarchive.gocfl.server.GocflServer;
import org.ocflarchive.gocfl.server.GocflServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * High-level Java client for interacting with the gocfl gRPC service.
 */
public class GocflClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GocflClient.class);

    private final ManagedChannel channel;
    private final GocflServiceBlockingStub blockingStub;
    private final GocflServiceStub asyncStub;
    private final GocflServiceFutureStub futureStub;
    private final GocflServer server;
    private final LogHandler defaultLogHandler;
    private volatile boolean closed = false;

    GocflClient(ManagedChannel channel, GocflServer server, LogHandler defaultLogHandler) {
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.blockingStub = GocflServiceGrpc.newBlockingStub(channel);
        this.asyncStub = GocflServiceGrpc.newStub(channel);
        this.futureStub = GocflServiceGrpc.newFutureStub(channel);
        this.server = server;
        this.defaultLogHandler = defaultLogHandler;
    }

    /**
     * Creates a new client builder.
     */
    public static GocflClientBuilder builder() {
        return new GocflClientBuilder();
    }

    /**
     * Connects to a running gocfl-grpc service on the given host and port.
     */
    public static GocflClient connect(String host, int port) {
        return builder().host(host).port(port).build();
    }

    /**
     * Connects to a running gocfl-grpc service using the given target string (e.g. "localhost:50051").
     */
    public static GocflClient connect(String target) {
        return builder().target(target).build();
    }

    /**
     * Starts a local gocfl-grpc server with default configuration and connects to it.
     */
    public static GocflClient startServer() {
        return startServer(GocflServerConfig.defaultConfig());
    }

    /**
     * Starts a local gocfl-grpc server with the provided configuration and connects to it.
     */
    public static GocflClient startServer(GocflServerConfig config) {
        GocflServer server = GocflServer.start(config);
        return builder()
                .host(server.getHost())
                .port(server.getPort())
                .server(server)
                .build();
    }

    // --- High-level Operations ---

    /**
     * Initializes an empty OCFL storage root at the specified path.
     */
    public InitResult init(String ocflPath) {
        return init(InitRequest.newBuilder().setOcflPath(ocflPath).build());
    }

    /**
     * Initializes an empty OCFL storage root.
     */
    public InitResult init(InitRequest request) {
        return init(request, defaultLogHandler);
    }

    /**
     * Initializes an empty OCFL storage root with a dedicated log handler callback.
     */
    public InitResult init(InitRequest request, LogHandler logHandler) {
        try {
            Iterator<InitResponse> stream = blockingStub.init(request);
            InitResult result = null;

            while (stream.hasNext()) {
                InitResponse resp = stream.next();
                if (resp.hasLog() && logHandler != null) {
                    logHandler.onLog(resp.getLog());
                }
                if (resp.hasResult()) {
                    result = resp.getResult();
                }
            }

            if (result == null) {
                throw new GocflException("Server closed Init stream without returning a result");
            }
            if (!result.getSuccess()) {
                throw new GocflOperationException("Init failed: " + result.getMessage());
            }
            return result;
        } catch (StatusRuntimeException e) {
            throw new GocflException("gRPC error during Init: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the raw stream iterator for the Init RPC.
     */
    public Iterator<InitResponse> initStream(InitRequest request) {
        return blockingStub.init(request);
    }

    /**
     * Adds a new object to an existing OCFL storage root.
     */
    public AddResult add(AddRequest request) {
        return add(request, defaultLogHandler);
    }

    /**
     * Adds a new object to an existing OCFL storage root with a dedicated log handler.
     */
    public AddResult add(AddRequest request, LogHandler logHandler) {
        try {
            Iterator<AddResponse> stream = blockingStub.add(request);
            AddResult result = null;

            while (stream.hasNext()) {
                AddResponse resp = stream.next();
                if (resp.hasLog() && logHandler != null) {
                    logHandler.onLog(resp.getLog());
                }
                if (resp.hasResult()) {
                    result = resp.getResult();
                }
            }

            if (result == null) {
                throw new GocflException("Server closed Add stream without returning a result");
            }
            if (!result.getSuccess()) {
                throw new GocflOperationException("Add failed: " + result.getMessage());
            }
            return result;
        } catch (StatusRuntimeException e) {
            throw new GocflException("gRPC error during Add: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the raw stream iterator for the Add RPC.
     */
    public Iterator<AddResponse> addStream(AddRequest request) {
        return blockingStub.add(request);
    }

    /**
     * Updates an existing object in an OCFL structure by adding a new version.
     */
    public UpdateResult update(UpdateRequest request) {
        return update(request, defaultLogHandler);
    }

    /**
     * Updates an existing object with a dedicated log handler.
     */
    public UpdateResult update(UpdateRequest request, LogHandler logHandler) {
        try {
            Iterator<UpdateResponse> stream = blockingStub.update(request);
            UpdateResult result = null;

            while (stream.hasNext()) {
                UpdateResponse resp = stream.next();
                if (resp.hasLog() && logHandler != null) {
                    logHandler.onLog(resp.getLog());
                }
                if (resp.hasResult()) {
                    result = resp.getResult();
                }
            }

            if (result == null) {
                throw new GocflException("Server closed Update stream without returning a result");
            }
            if (!result.getSuccess()) {
                throw new GocflOperationException("Update failed: " + result.getMessage());
            }
            return result;
        } catch (StatusRuntimeException e) {
            throw new GocflException("gRPC error during Update: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the raw stream iterator for the Update RPC.
     */
    public Iterator<UpdateResponse> updateStream(UpdateRequest request) {
        return blockingStub.update(request);
    }

    /**
     * Initializes an OCFL structure and adds an initial object.
     */
    public CreateResult create(CreateRequest request) {
        return create(request, defaultLogHandler);
    }

    /**
     * Initializes an OCFL structure and adds an initial object with a dedicated log handler.
     */
    public CreateResult create(CreateRequest request, LogHandler logHandler) {
        try {
            Iterator<CreateResponse> stream = blockingStub.create(request);
            CreateResult result = null;

            while (stream.hasNext()) {
                CreateResponse resp = stream.next();
                if (resp.hasLog() && logHandler != null) {
                    logHandler.onLog(resp.getLog());
                }
                if (resp.hasResult()) {
                    result = resp.getResult();
                }
            }

            if (result == null) {
                throw new GocflException("Server closed Create stream without returning a result");
            }
            if (!result.getSuccess()) {
                throw new GocflOperationException("Create failed: " + result.getMessage());
            }
            return result;
        } catch (StatusRuntimeException e) {
            throw new GocflException("gRPC error during Create: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the raw stream iterator for the Create RPC.
     */
    public Iterator<CreateResponse> createStream(CreateRequest request) {
        return blockingStub.create(request);
    }

    /**
     * Validates an OCFL storage root at the specified path.
     */
    public ValidateResult validate(String ocflPath) {
        return validate(ValidateRequest.newBuilder().setOcflPath(ocflPath).build());
    }

    /**
     * Validates an OCFL storage root or specific object.
     */
    public ValidateResult validate(ValidateRequest request) {
        return validate(request, defaultLogHandler);
    }

    /**
     * Validates an OCFL storage root or specific object with a dedicated log handler.
     */
    public ValidateResult validate(ValidateRequest request, LogHandler logHandler) {
        try {
            Iterator<ValidateResponse> stream = blockingStub.validate(request);
            ValidateResult result = null;

            while (stream.hasNext()) {
                ValidateResponse resp = stream.next();
                if (resp.hasLog() && logHandler != null) {
                    logHandler.onLog(resp.getLog());
                }
                if (resp.hasResult()) {
                    result = resp.getResult();
                }
            }

            if (result == null) {
                throw new GocflException("Server closed Validate stream without returning a result");
            }
            return result;
        } catch (StatusRuntimeException e) {
            throw new GocflException("gRPC error during Validate: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the raw stream iterator for the Validate RPC.
     */
    public Iterator<ValidateResponse> validateStream(ValidateRequest request) {
        return blockingStub.validate(request);
    }

    /**
     * Requests the gocfl-grpc server to shut down.
     */
    public ShutdownResponse shutdown(String reason, boolean force) {
        return shutdown(ShutdownRequest.newBuilder().setReason(reason).setForce(force).build());
    }

    /**
     * Requests the gocfl-grpc server to shut down.
     */
    public ShutdownResponse shutdown(ShutdownRequest request) {
        try {
            return blockingStub.shutdown(request);
        } catch (StatusRuntimeException e) {
            throw new GocflException("gRPC error during Shutdown: " + e.getMessage(), e);
        }
    }

    // --- Accessors ---

    public ManagedChannel getChannel() {
        return channel;
    }

    public GocflServiceBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public GocflServiceStub getAsyncStub() {
        return asyncStub;
    }

    public GocflServiceFutureStub getFutureStub() {
        return futureStub;
    }

    public Optional<GocflServer> getServer() {
        return Optional.ofNullable(server);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;

        log.info("Closing GocflClient channel...");
        try {
            channel.shutdown();
            if (!channel.awaitTermination(3, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
        }

        if (server != null) {
            log.info("Closing managed GocflServer...");
            server.close();
        }
    }
}
