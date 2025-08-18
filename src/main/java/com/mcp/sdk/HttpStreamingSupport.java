package com.mcp.sdk;

import io.vertx.core.json.JsonObject;

/**
 * Interface for tools that support HTTP streaming.
 * Tools can implement this to handle HTTP chunked streaming directly.
 */
public interface HttpStreamingSupport {
    
    /**
     * Handle HTTP streaming request.
     * The tool should use the streamingId to access the HTTP streaming context
     * and write chunks directly to the HTTP response.
     * 
     * @param arguments tool arguments
     * @param requestId JSON-RPC request ID
     * @param sessionId session ID for logging
     * @param streamingId unique ID to access the HTTP streaming context
     */
    void handleHttpStreaming(JsonObject arguments, Object requestId, String sessionId, String streamingId);
} 