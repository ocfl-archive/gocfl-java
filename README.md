# gocfl-java

Java-Client-Bibliothek und Wrapper für [gocfl](https://github.com/ocfl-archive/gocfl) über [gocfl-grpc](https://github.com/ocfl-archive/gocfl-grpc).

`gocfl-java` ermöglicht die nahtlose Verwaltung von OCFL (Oxford Common File Layout) Storage Roots und Objekten aus Java heraus. Die Bibliothek kann entweder eine bestehende `gocfl-grpc`-Serverinstanz ansprechen oder den lokalen `gocfl-grpc`-Prozess automatisch starten, überwachen, dynamische Ports zuweisen und beim Schließen des Clients sauber herunterfahren.

---

## Features

- **Automatisches Server-Management (`GocflServer`)**:
  - Automatischer Start des Go-Binärprozesses `gocflgrpc`.
  - Dynamische Port-Zuweisung (`port = 0`) zur Vermeidung von Port-Kollisionen.
  - Automatische Erkennung der gebundenen Adresse via Prozess-Output.
  - Sichere Lebenszyklusverwaltung (inkl. JVM Shutdown-Hook und Graceful Shutdown).
  - Umleitung der Server-Logs in SLF4J.
- **Benutzerfreundlicher Java-Client (`GocflClient`)**:
  - Synchrone Methoden (`init`, `add`, `update`, `create`, `validate`, `shutdown`).
  - Streaming-Methoden (`initStream`, `addStream`, `updateStream`, `createStream`, `validateStream`).
  - Echtzeit-Streaming von Log-Events (`LogHandler` / `Consumer<LogEntry>`).
  - Direkter Zugriff auf generierte Protobuf- / gRPC-Stubs (`BlockingStub`, `Stub`, `FutureStub`, `ManagedChannel`).
- **Einfache Fassade (`Gocfl`)**:
  - Schneller Einstieg über statische Hilfsmethoden und Builder.

---

## Voraussetzungen

- **Java 17+**
- **gocfl-grpc Binärdatei** (`gocflgrpc` bzw. `gocflgrpc.exe`), z. B. unter `C:\daten\go\dev\gocfl-grpc\gocflgrpc.exe` oder im System-`PATH` bzw. via `GOCFL_GRPC_PATH` / `-Dgocfl.grpc.path`.

---

## Schnelleinstieg

### 1. Lokalen Server starten und Storage Root initialisieren

```java
import org.ocflarchive.gocfl.Gocfl;
import org.ocflarchive.gocfl.client.GocflClient;
import org.ocflarchive.gocfl.proto.*;

public class Example {
    public static void main(String[] args) {
        // Startet gocfl-grpc im Hintergrund und verbindet den Client automatisch
        try (GocflClient client = Gocfl.startServer()) {
            
            // 1. OCFL Storage Root initialisieren
            InitResult initResult = client.init("/pfad/zu/ocfl-storage");
            System.out.println("Init erfolgreich: " + initResult.getSuccess());

            // 2. Objekt mit Version 1 hinzufügen
            AddRequest addReq = AddRequest.newBuilder()
                    .setOcflPath("/pfad/zu/ocfl-storage")
                    .setSrcPath("/pfad/zu/quelldaten")
                    .setObjectId("urn:example:obj001")
                    .setMessage("Initial commit")
                    .setUser(Gocfl.user("Alice", "alice@example.com"))
                    .build();

            AddResult addResult = client.add(addReq, logEntry -> {
                System.out.println("[Server Log] " + logEntry.getMessage());
            });
            System.out.println("Objekt angelegt: " + addResult.getObjectId() + ", Version: " + addResult.getVersion());

            // 3. Storage Root validieren
            ValidateResult valResult = client.validate("/pfad/zu/ocfl-storage");
            System.out.println("OCFL gültig: " + valResult.getIsValid());
            
        } // Am Ende von try-with-resources wird der Channel geschlossen und der Serverprozess gestoppt
    }
}
```

---

### 2. Verbindung zu einem bereits laufenden gRPC-Server

```java
try (GocflClient client = Gocfl.connect("localhost", 50051)) {
    ValidateResult valResult = client.validate("/pfad/zu/ocfl-storage");
    System.out.println("Gültig: " + valResult.getIsValid());
}
```

---

### 3. Benutzerdefinierte Server-Konfiguration

```java
GocflServerConfig serverConfig = GocflServerConfig.builder()
        .executablePath("C:\\daten\\go\\dev\\gocfl-grpc\\gocflgrpc.exe")
        .port(0) // 0 = dynamischer Port
        .logLevel("INFO")
        .s3Endpoint("http://localhost:9000")
        .s3AccessKeyId("minioadmin")
        .s3SecretAccessKey("minioadmin")
        .s3Region("us-east-1")
        .build();

try (GocflClient client = Gocfl.startServer(serverConfig)) {
    // Client-Aufrufe
}
```

---

### 4. Direkter Zugriff auf Low-Level gRPC-Stubs

Falls spezielle gRPC-Optionen oder asynchrone / Reactive-Aufrufe benötigt werden:

```java
try (GocflClient client = Gocfl.startServer()) {
    GocflServiceGrpc.GocflServiceBlockingStub blockingStub = client.getBlockingStub();
    GocflServiceGrpc.GocflServiceStub asyncStub = client.getAsyncStub();
    GocflServiceGrpc.GocflServiceFutureStub futureStub = client.getFutureStub();
    ManagedChannel channel = client.getChannel();
}
```

---

## Build & Tests

Das Projekt verwendet Gradle:

```bash
# Projekt kompilieren
./gradlew compileJava

# Integrationstests ausführen
./gradlew test

# JAR-Dateien (inkl. Sources & Javadoc) erstellen
./gradlew build
```
