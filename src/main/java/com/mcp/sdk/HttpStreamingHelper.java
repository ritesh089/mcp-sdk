package com.mcp.sdk;

import io.vertx.core.json.JsonObject;

/**
 * Helper class for tools to access HTTP streaming functionality.
 * This uses a callback mechanism to bridge between the SDK and the framework.
 */
public class HttpStreamingHelper {
    
    private static HttpStreamingProvider provider;
    
    /**
     * Interface for the framework to implement HTTP streaming functionality
     */
    public interface HttpStreamingProvider {
        boolean writeChunk(String streamingId, JsonObject chunk);
        boolean writeJsonRpcChunk(String streamingId, JsonObject result);
        boolean writeJsonRpcResult(String streamingId, JsonObject result);
        boolean writeJsonRpcError(String streamingId, int code, String message);
        boolean endStream(String streamingId);
        Object getRequestId(String streamingId);
        String getSessionId(String streamingId);
    }
    
    /**
     * Set the HTTP streaming provider (called by the framework)
     * @param streamingProvider the provider implementation
     */
    public static void setProvider(HttpStreamingProvider streamingProvider) {
        provider = streamingProvider;
    }
    
    /**
     * Write a JSON chunk to the HTTP response stream
     * @param streamingId the streaming context ID
     * @param jsonObject the JSON object to write as a chunk
     * @return true if successful, false if streaming context not found
     */
    public static boolean writeChunk(String streamingId, JsonObject jsonObject) {
        return provider != null && provider.writeChunk(streamingId, jsonObject);
    }
    
    /**
     * Write a JSON-RPC response chunk with the correct request ID
     * @param streamingId the streaming context ID
     * @param result the result object to wrap in JSON-RPC format
     * @return true if successful, false if streaming context not found
     */
    public static boolean writeJsonRpcChunk(String streamingId, JsonObject result) {
        return provider != null && provider.writeJsonRpcChunk(streamingId, result);
    }
    
    /**
     * Write a JSON-RPC result (typically the final response)
     * @param streamingId the streaming context ID
     * @param result the final result object
     * @return true if successful, false if streaming context not found
     */
    public static boolean writeJsonRpcResult(String streamingId, JsonObject result) {
        return provider != null && provider.writeJsonRpcResult(streamingId, result);
    }
    
    /**
     * Write a JSON-RPC error response
     * @param streamingId the streaming context ID
     * @param code error code
     * @param message error message
     * @return true if successful, false if streaming context not found
     */
    public static boolean writeJsonRpcError(String streamingId, int code, String message) {
        return provider != null && provider.writeJsonRpcError(streamingId, code, message);
    }
    
    /**
     * End the HTTP streaming response
     * @param streamingId the streaming context ID
     * @return true if successful, false if streaming context not found
     */
    public static boolean endStream(String streamingId) {
        return provider != null && provider.endStream(streamingId);
    }
    
    /**
     * Get the request ID for this streaming context
     * @param streamingId the streaming context ID
     * @return the JSON-RPC request ID or null if context not found
     */
    public static Object getRequestId(String streamingId) {
        return provider != null ? provider.getRequestId(streamingId) : null;
    }
    
    /**
     * Get the session ID for this streaming context
     * @param streamingId the streaming context ID
     * @return the session ID or null if context not found
     */
    public static String getSessionId(String streamingId) {
        return provider != null ? provider.getSessionId(streamingId) : null;
    }
} 