package org.ocflarchive.gocfl.client;

import org.ocflarchive.gocfl.proto.LogEntry;

/**
 * Callback interface invoked when a log message is streamed from the gocfl-grpc server during an operation.
 */
@FunctionalInterface
public interface LogHandler {

    /**
     * Handles a single streamed log entry.
     *
     * @param entry the log entry received from the server
     */
    void onLog(LogEntry entry);
}
