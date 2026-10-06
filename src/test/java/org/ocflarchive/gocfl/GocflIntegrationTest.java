package org.ocflarchive.gocfl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ocflarchive.gocfl.client.GocflClient;
import org.ocflarchive.gocfl.proto.AddRequest;
import org.ocflarchive.gocfl.proto.AddResult;
import org.ocflarchive.gocfl.proto.CreateRequest;
import org.ocflarchive.gocfl.proto.CreateResponse;
import org.ocflarchive.gocfl.proto.CreateResult;
import org.ocflarchive.gocfl.proto.InitRequest;
import org.ocflarchive.gocfl.proto.InitResult;
import org.ocflarchive.gocfl.proto.LogEntry;
import org.ocflarchive.gocfl.proto.ShutdownResponse;
import org.ocflarchive.gocfl.proto.UpdateRequest;
import org.ocflarchive.gocfl.proto.UpdateResult;
import org.ocflarchive.gocfl.proto.User;
import org.ocflarchive.gocfl.proto.ValidateRequest;
import org.ocflarchive.gocfl.proto.ValidateResult;
import org.ocflarchive.gocfl.server.GocflServer;
import org.ocflarchive.gocfl.server.GocflServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GocflIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(GocflIntegrationTest.class);

    @TempDir
    Path tempDir;

    private GocflClient client;

    @BeforeEach
    void setUp() {
        GocflServerConfig config = GocflServerConfig.builder()
                .port(0) // dynamic port
                .logLevel("DEBUG")
                .allowApiShutdown(true)
                .build();
        client = Gocfl.startServer(config);
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testServerStartedAndConnected() {
        assertThat(client).isNotNull();
        assertThat(client.getServer()).isPresent();

        GocflServer server = client.getServer().get();
        assertThat(server.isAlive()).isTrue();
        assertThat(server.getPort()).isGreaterThan(0);
        log.info("Server is running on target: {}", server.getTarget());
    }

    @Test
    void testInitCreateUpdateAndValidate() throws IOException {
        Path ocflRoot = tempDir.resolve("ocfl-storage");
        Path srcFolder = tempDir.resolve("source-data");
        Files.createDirectories(srcFolder);

        Path testFile1 = srcFolder.resolve("document.txt");
        Files.writeString(testFile1, "Hello OCFL World version 1");

        List<LogEntry> logs = new ArrayList<>();

        // 1. Init OCFL storage root
        log.info("Initializing OCFL root at: {}", ocflRoot);
        InitRequest initReq = InitRequest.newBuilder()
                .setOcflPath(ocflRoot.toString())
                .setOcflVersion("1.1")
                .build();

        InitResult initResult = client.init(initReq, logs::add);
        assertThat(initResult).isNotNull();
        assertThat(initResult.getSuccess()).isTrue();
        assertThat(logs).isNotEmpty();

        // 2. Add object (v1)
        log.info("Adding object to OCFL root...");
        User user = Gocfl.user("Alice", "alice@example.com");
        AddRequest addReq = AddRequest.newBuilder()
                .setOcflPath(ocflRoot.toString())
                .setSrcPath(srcFolder.toString())
                .setObjectId("urn:example:doc001")
                .setMessage("Initial commit of document")
                .setUser(user)
                .build();

        AddResult addResult = client.add(addReq);
        assertThat(addResult).isNotNull();
        assertThat(addResult.getSuccess()).isTrue();
        assertThat(addResult.getObjectId()).isEqualTo("urn:example:doc001");
        assertThat(addResult.getVersion()).isEqualTo("v1");

        // 3. Validate
        log.info("Validating OCFL root...");
        ValidateRequest valReq = Gocfl.validateRequest(ocflRoot.toString());

        ValidateResult valResult = client.validate(valReq);
        assertThat(valResult).isNotNull();
        assertThat(valResult.getIsValid()).isTrue();
        assertThat(valResult.getErrorsList()).isEmpty();

        // 4. Update object (v2)
        log.info("Updating object with v2...");
        Files.writeString(testFile1, "Hello OCFL World version 2 updated");
        Path testFile2 = srcFolder.resolve("extra.txt");
        Files.writeString(testFile2, "Extra file for version 2");

        UpdateRequest updateReq = UpdateRequest.newBuilder()
                .setOcflPath(ocflRoot.toString())
                .setSrcPath(srcFolder.toString())
                .setObjectId("urn:example:doc001")
                .setMessage("Updated document and added extra.txt")
                .setUser(user)
                .build();

        UpdateResult updateResult = client.update(updateReq);
        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getSuccess()).isTrue();
        assertThat(updateResult.getObjectId()).isEqualTo("urn:example:doc001");
        assertThat(updateResult.getVersion()).isEqualTo("v2");

        // 5. Re-validate
        ValidateResult valResultAfterUpdate = client.validate(ocflRoot.toString());
        assertThat(valResultAfterUpdate.getIsValid()).isTrue();
        assertThat(valResultAfterUpdate.getErrorsList()).isEmpty();
    }

    @Test
    void testCreateOperationAndStreaming() throws IOException {
        Path ocflRoot = tempDir.resolve("ocfl-create-test");
        Path srcFolder = tempDir.resolve("create-src");
        Files.createDirectories(srcFolder);
        Files.writeString(srcFolder.resolve("data.json"), "{\"status\":\"ok\"}");

        User user = Gocfl.user("Bob", "bob@example.com");
        CreateRequest createReq = CreateRequest.newBuilder()
                .setOcflPath(ocflRoot.toString())
                .setSrcPath(srcFolder.toString())
                .setObjectId("urn:example:obj-create-001")
                .setMessage("Create root and object in one step")
                .setUser(user)
                .build();

        // Test stream
        Iterator<CreateResponse> stream = client.createStream(createReq);
        CreateResult result = null;
        int messageCount = 0;
        while (stream.hasNext()) {
            CreateResponse resp = stream.next();
            messageCount++;
            if (resp.hasResult()) {
                result = resp.getResult();
            }
        }

        assertThat(messageCount).isGreaterThan(0);
        assertThat(result).isNotNull();
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getObjectId()).isEqualTo("urn:example:obj-create-001");
        assertThat(result.getVersion()).isEqualTo("v1");

        // Validate created root
        ValidateResult val = client.validate(ocflRoot.toString());
        assertThat(val.getIsValid()).isTrue();
    }

    @Test
    void testApiShutdown() {
        ShutdownResponse resp = client.shutdown("Test shutdown", false);
        assertThat(resp).isNotNull();
        assertThat(resp.getSuccess()).isTrue();
    }
}
