package com.mcp.sdk;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Abstract base class for MCP tools that provides common functionality:
 * - Event bus registration and message handling
 * - Consistent logging and error handling
 * - Input validation framework
 * - Standardized response format
 * 
 * This is the lightweight SDK version that doesn't depend on server framework.
 * Tools should extend this class and implement:
 * - getToolName(): Return the tool's name
 * - handleToolCall(JsonObject arguments): Process the tool request
 */
public abstract class MCPTool extends AbstractVerticle {

    private ToolEventLogger logger = ToolEventLogger.NOOP;

    /**
     * Set the event logger implementation (called by the framework)
     * @param logger the logger implementation
     */
    public void setEventLogger(ToolEventLogger logger) {
        this.logger = logger != null ? logger : ToolEventLogger.NOOP;
    }

    /**
     * Get the event logger instance for this tool
     * @return the event logger
     */
    protected ToolEventLogger getEventLogger() {
        return this.logger;
    }

    /**
     * Get the name of this tool (used for event bus address and logging)
     * @return the tool name
     */
    protected abstract String getToolName();

    /**
     * Handle the actual tool execution logic with context
     * @param arguments the input arguments from the MCP call
     * @param context MCPContext containing request metadata, correlation IDs, and tracing information
     * @return JsonObject response to send back to the client
     * @throws Exception if the tool execution fails
     */
    protected JsonObject handleToolCall(JsonObject arguments, MCPContext context) throws Exception {
        // Default implementation calls the legacy method for backward compatibility
        return handleToolCall(arguments);
    }
    
    /**
     * Handle the actual tool execution logic (legacy method)
     * @deprecated Use {@link #handleToolCall(JsonObject, MCPContext)} instead for access to context information
     * @param arguments the input arguments from the MCP call
     * @return JsonObject response to send back to the client
     * @throws Exception if the tool execution fails
     */
    @Deprecated
    protected JsonObject handleToolCall(JsonObject arguments) throws Exception {
        throw new UnsupportedOperationException("Implement handleToolCall(JsonObject, MCPContext) instead");
    }

    /**
     * Override this method to perform custom validation on input arguments
     * Default implementation does no validation
     * @param arguments the input arguments to validate
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateArguments(JsonObject arguments) throws IllegalArgumentException {
        // Default: no validation
    }

    /**
     * Override this method to perform custom initialization
     * Called after event bus registration
     */
    protected void onToolInitialized() {
        // Default: no custom initialization
    }
    
    /**
     * Create MCPContext from the incoming request data.
     * Extracts correlation ID, session information, and other metadata.
     */
    private MCPContext createContextFromRequest(JsonObject request) {
        DefaultMCPContext.Builder builder = DefaultMCPContext.builder()
            .startTime(System.currentTimeMillis());
        
        // Extract context data from request if available
        if (request.containsKey("correlationId")) {
            builder.correlationId(request.getString("correlationId"));
        }
        if (request.containsKey("mcpId")) {
            builder.mcpId(request.getString("mcpId"));
        }
        if (request.containsKey("sessionId")) {
            builder.sessionId(request.getString("sessionId"));
        }
        if (request.containsKey("method")) {
            builder.method(request.getString("method"));
        }
        if (request.containsKey("requestId")) {
            builder.requestId(request.getValue("requestId"));
        }
        if (request.containsKey("clientAddress")) {
            builder.clientAddress(request.getString("clientAddress"));
        }
        if (request.containsKey("streaming")) {
            builder.streaming(request.getBoolean("streaming", false));
        }
        
        // Add any additional metadata
        JsonObject metadata = request.getJsonObject("metadata", new JsonObject());
        builder.metadata(metadata);
        
        return builder.build();
    }

    @Override
    public final void start(Promise<Void> startPromise) {
        String toolName = getToolName();
        String eventBusAddress = "tools." + toolName;
        
        try {
            // Register event bus consumer
            vertx.eventBus().consumer(eventBusAddress, this::handleMessage);
            
            // Log successful initialization
            logger.logToolExecution(toolName, "startup", "success", null, 
                toolName + " tool verticle initialized", null);

            
            // Call custom initialization hook
            onToolInitialized();
            
            startPromise.complete();
        } catch (Exception e) {
            logger.logToolExecution(toolName, "startup", "failed", null, 
                "Failed to initialize: " + e.getMessage(), null);
            startPromise.fail(e);
        }
    }

    protected void handleMessage(Message<JsonObject> message) {
        String toolName = getToolName();
        JsonObject request = message.body();
        JsonObject arguments = request.getJsonObject("arguments", new JsonObject());
        
        // Extract context information from the request
        MCPContext context = createContextFromRequest(request);
        
        try {
            logger.logToolExecution(toolName, "start", "processing", arguments, null, null);
            
            // Validate arguments
            validateArguments(arguments);
            logger.logToolExecution(toolName, "validation", "success", arguments, null, null);
            
            // Execute tool logic with context
            JsonObject response = handleToolCall(arguments, context);
            
            // Ensure response has proper format
            if (response == null) {
                response = createSuccessResponse("Tool executed successfully");
            }
            
            logger.logToolExecution(toolName, "execution", "success", arguments, 
                "Tool execution completed", null);
            
            message.reply(response);
            
        } catch (IllegalArgumentException e) {
            // Validation error
            logger.logToolExecution(toolName, "validation", "failed", arguments, 
                e.getMessage(), null);
            JsonObject error = createErrorResponse(-32602, e.getMessage());
            message.reply(error);
            
        } catch (Exception e) {
            // Execution error
            logger.logToolExecution(toolName, "execution", "exception", arguments, 
                "Error: " + e.getMessage(), null);
            JsonObject error = createErrorResponse(-32603, toolName + " failed: " + e.getMessage());
            message.reply(error);
        }
    }

    /**
     * Create a standard success response
     * @param text the response text
     * @return formatted JsonObject response
     */
    protected JsonObject createSuccessResponse(String text) {
        return new JsonObject()
            .put("content", new JsonArray()
                .add(new JsonObject()
                    .put("type", "text")
                    .put("text", text)));
    }

    /**
     * Create a standard success response with multiple content items
     * @param contentItems array of content items
     * @return formatted JsonObject response
     */
    protected JsonObject createSuccessResponse(JsonArray contentItems) {
        return new JsonObject().put("content", contentItems);
    }

    /**
     * Create a standard error response in MCP protocol format
     * @param code the error code
     * @param message the error message
     * @return formatted JsonObject error response
     */
    protected JsonObject createErrorResponse(int code, String message) {
        return new JsonObject()
            .put("content", new JsonArray()
                .add(new JsonObject()
                    .put("type", "text")
                    .put("text", "Error: " + message)));
    }

    /**
     * Utility method to get a required string parameter
     * @param arguments the arguments object
     * @param paramName the parameter name
     * @return the parameter value
     * @throws IllegalArgumentException if parameter is missing or not a string
     */
    protected String getRequiredString(JsonObject arguments, String paramName) {
        String value = arguments.getString(paramName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' is required");
        }
        return value;
    }

    /**
     * Utility method to get a required integer parameter
     * @param arguments the arguments object
     * @param paramName the parameter name
     * @return the parameter value
     * @throws IllegalArgumentException if parameter is missing or not an integer
     */
    protected int getRequiredInteger(JsonObject arguments, String paramName) {
        Object value = arguments.getValue(paramName);
        if (value == null) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' is required");
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' must be a valid integer");
        }
    }

    /**
     * Utility method to get an optional string parameter with default value
     * @param arguments the arguments object
     * @param paramName the parameter name
     * @param defaultValue the default value if parameter is missing
     * @return the parameter value or default
     */
    protected String getOptionalString(JsonObject arguments, String paramName, String defaultValue) {
        String value = arguments.getString(paramName);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    /**
     * Utility method to get an optional integer parameter with default value
     * @param arguments the arguments object
     * @param paramName the parameter name
     * @param defaultValue the default value if parameter is missing
     * @return the parameter value or default
     */
    protected int getOptionalInteger(JsonObject arguments, String paramName, int defaultValue) {
        Object value = arguments.getValue(paramName);
        if (value == null) {
            return defaultValue;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            } else {
                return Integer.parseInt(value.toString());
            }
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
} 