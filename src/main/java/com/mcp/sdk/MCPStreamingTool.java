package com.mcp.sdk;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

/**
 * Abstract base class for asynchronous/streaming MCP tools.
 * Extends MCPTool to handle async tool execution and HTTP streaming.
 */
public abstract class MCPStreamingTool extends MCPTool {

    /**
     * Handle asynchronous tool calls (standard MCP mode)
     * @param arguments tool arguments
     * @param message event bus message for async reply
     * @throws Exception if tool execution fails
     */
    protected abstract void handleAsyncToolCall(JsonObject arguments, Message<JsonObject> message) throws Exception;

    /**
     * Handle HTTP streaming requests (when Accept: text/plain header is present)
     * Override this method to support HTTP streaming.
     * 
     * @param arguments tool arguments
     * @param requestId JSON-RPC request ID
     * @param sessionId HTTP session ID for logging
     * @return JsonObject with httpStreamingSupported=true and streamingAddress for the framework to use
     * @throws Exception if streaming setup fails
     */
    protected JsonObject handleHttpStreamingRequest(JsonObject arguments, Object requestId, String sessionId) throws Exception {
        // Default implementation: no HTTP streaming support
        return new JsonObject()
            .put("httpStreamingSupported", false)
            .put("fallbackToRegular", true);
    }

    /**
     * Override to prevent direct use of handleToolCall in streaming tools
     */
    @Override
    protected final JsonObject handleToolCall(JsonObject arguments) throws Exception {
        throw new UnsupportedOperationException("Streaming tools should use handleAsyncToolCall or handleHttpStreamingRequest");
    }

    @Override
    protected final void handleMessage(Message<JsonObject> message) {
        String toolName = getToolName();
        JsonObject request = message.body();
        JsonObject arguments = request.getJsonObject("arguments", new JsonObject());
        
        try {
            ToolEventLogger logger = getEventLogger();
            logger.logToolExecution(toolName, "start", "processing", arguments, null, null);
            
            // Validate arguments
            validateArguments(arguments);
            logger.logToolExecution(toolName, "validation", "success", arguments, null, null);
            
            // Check if this is an HTTP streaming request
            if (request.containsKey("httpStreaming") && request.getBoolean("httpStreaming", false)) {
                Object requestId = request.getValue("requestId");
                String sessionId = request.getString("sessionId");
                String streamingId = request.getString("streamingId");
                
                // Check if this tool supports HTTP streaming
                if (this instanceof HttpStreamingSupport) {
                    logger.logToolExecution(toolName, "streaming", "supported", arguments, 
                        "Tool supports HTTP streaming", null);
                    
                    // Reply immediately that we support HTTP streaming
                    JsonObject streamingResponse = new JsonObject()
                        .put("httpStreamingSupported", true);
                    message.reply(streamingResponse);
                    
                    // Handle streaming asynchronously
                    vertx.runOnContext(v -> {
                        try {
                            ((HttpStreamingSupport) this).handleHttpStreaming(arguments, requestId, sessionId, streamingId);
                        } catch (Exception e) {
                            logger.logToolExecution(toolName, "streaming", "exception", arguments, 
                                "HTTP streaming error: " + e.getMessage(), null);
                            
                            // Write error to stream and end it
                            HttpStreamingHelper.writeJsonRpcError(streamingId, -32603, 
                                "Streaming failed: " + e.getMessage());
                            HttpStreamingHelper.endStream(streamingId);
                        }
                    });
                    
                } else {
                    // Tool doesn't support HTTP streaming, fall back to regular async call
                    logger.logToolExecution(toolName, "streaming", "unsupported", arguments, 
                        "Tool doesn't support HTTP streaming, falling back to regular call", null);
                    
                    JsonObject fallbackResponse = handleHttpStreamingRequest(arguments, requestId, sessionId);
                    message.reply(fallbackResponse);
                }
            } else {
                // Handle as regular async tool call
                handleAsyncToolCall(arguments, message);
            }
            
        } catch (IllegalArgumentException e) {
            // Validation error
            ToolEventLogger logger = getEventLogger();
            logger.logToolExecution(toolName, "validation", "failed", arguments, 
                e.getMessage(), null);
            JsonObject error = createErrorResponse(-32602, e.getMessage());
            message.reply(error);
            
        } catch (Exception e) {
            // Execution error
            ToolEventLogger logger = getEventLogger();
            logger.logToolExecution(toolName, "execution", "exception", arguments, 
                "Error: " + e.getMessage(), null);
            JsonObject error = createErrorResponse(-32603, toolName + " failed: " + e.getMessage());
            message.reply(error);
        }
    }
} 