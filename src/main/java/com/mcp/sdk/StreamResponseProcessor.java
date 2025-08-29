package com.mcp.sdk;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Processor for handling Stream&lt;T&gt; return types from MCP tools.
 * Provides smart routing between streaming and POJO collection based on client request.
 */
public class StreamResponseProcessor {
    
    /**
     * Process a Stream&lt;T&gt; return from a tool method.
     * Decides whether to stream in real-time or collect to POJO based on context.
     * 
     * @param stream the Stream returned by the tool method
     * @param context the MCP request context
     * @param method the tool method that returned the stream
     * @return JsonObject response (either streaming acknowledgment or collected POJO)
     */
    public static JsonObject processStreamResponse(Stream<?> stream, MCPContext context, Method method) {
        if (shouldStreamRealTime(context)) {
            return handleRealTimeStreaming(stream, context, method);
        } else {
            return handleCollectedResponse(stream, context, method);
        }
    }
    
    /**
     * Determine if this request should stream in real-time vs collect to POJO.
     */
    private static boolean shouldStreamRealTime(MCPContext context) {
        return context.acceptsStreaming() || 
               context.isStreaming() ||
               context.getAttribute("forceStreaming").map(v -> (Boolean) v).orElse(false);
    }
    
    /**
     * Handle real-time streaming of Stream&lt;T&gt; items.
     */
    private static JsonObject handleRealTimeStreaming(Stream<?> stream, MCPContext context, Method method) {
        String streamingId = context.getStreamingId().orElse(null);
        
        if (streamingId == null) {
            throw new IllegalStateException("Streaming ID required for real-time streaming");
        }
        
        // Start streaming asynchronously
        if (context.isHttp2()) {
            streamWithHttp2(stream, streamingId, context);
        } else {
            streamWithHttp1(stream, streamingId, context);
        }
        
        // Return immediate acknowledgment
        return new JsonObject()
            .put("httpStreamingSupported", true)
            .put("streamingId", streamingId)
            .put("protocol", context.isHttp2() ? "HTTP/2" : "HTTP/1.1")
            .put("message", "Stream started - data will be sent in real-time");
    }
    
    /**
     * Handle collection of Stream&lt;T&gt; to POJO response.
     */
    private static JsonObject handleCollectedResponse(Stream<?> stream, MCPContext context, Method method) {
        try {
            // Collect stream to list
            List<?> collected = stream.collect(Collectors.toList());
            
            // Use existing ResponseProcessor for POJO handling
            ToolResult toolResult = ResponseProcessor.processResponse(collected, method);
            return toolResult.toJsonObject();
            
        } catch (Exception e) {
            ToolResult errorResult = ResponseProcessor.processGenericException(e, method);
            return errorResult.toJsonObject();
        }
    }
    
    /**
     * Stream items using HTTP/2 multiplexing and flow control.
     */
    private static void streamWithHttp2(Stream<?> stream, String streamingId, MCPContext context) {
        new Thread(() -> {
            try {
                stream.forEach(item -> {
                    JsonObject chunk = new JsonObject()
                        .put("type", "stream_item")
                        .put("data", item)
                        .put("timestamp", System.currentTimeMillis())
                        .put("protocol", "HTTP/2");
                    
                    // Use HTTP/2 flow control
                    boolean success = HttpStreamingHelper.writeJsonRpcChunk(streamingId, chunk);
                    if (!success) {
                        throw new RuntimeException("Failed to write HTTP/2 chunk");
                    }
                });
                
                // Send completion
                JsonObject completion = new JsonObject()
                    .put("type", "stream_complete")
                    .put("timestamp", System.currentTimeMillis())
                    .put("message", "Stream completed successfully");
                
                HttpStreamingHelper.writeJsonRpcResult(streamingId, completion);
                HttpStreamingHelper.endStream(streamingId);
                
            } catch (Exception e) {
                HttpStreamingHelper.writeJsonRpcError(streamingId, -32603, 
                    "Streaming error: " + e.getMessage());
                HttpStreamingHelper.endStream(streamingId);
            }
        }, "HTTP2-Stream-" + streamingId).start();
    }
    
    /**
     * Stream items using HTTP/1.1 chunked transfer.
     */
    private static void streamWithHttp1(Stream<?> stream, String streamingId, MCPContext context) {
        new Thread(() -> {
            try {
                stream.forEach(item -> {
                    JsonObject chunk = new JsonObject()
                        .put("type", "stream_item")
                        .put("data", item)
                        .put("timestamp", System.currentTimeMillis())
                        .put("protocol", "HTTP/1.1");
                    
                    boolean success = HttpStreamingHelper.writeJsonRpcChunk(streamingId, chunk);
                    if (!success) {
                        throw new RuntimeException("Failed to write HTTP/1.1 chunk");
                    }
                });
                
                // Send completion
                JsonObject completion = new JsonObject()
                    .put("type", "stream_complete")
                    .put("timestamp", System.currentTimeMillis())
                    .put("message", "Stream completed successfully");
                
                HttpStreamingHelper.writeJsonRpcResult(streamingId, completion);
                HttpStreamingHelper.endStream(streamingId);
                
            } catch (Exception e) {
                HttpStreamingHelper.writeJsonRpcError(streamingId, -32603, 
                    "Streaming error: " + e.getMessage());
                HttpStreamingHelper.endStream(streamingId);
            }
        }, "HTTP1-Stream-" + streamingId).start();
    }
    
    /**
     * Check if a return type represents a streamable result.
     */
    public static boolean isStreamableType(Class<?> returnType) {
        return Stream.class.isAssignableFrom(returnType) ||
               java.util.concurrent.Flow.Publisher.class.isAssignableFrom(returnType);
        // Future: Add support for Flux, Observable, etc.
    }
}
