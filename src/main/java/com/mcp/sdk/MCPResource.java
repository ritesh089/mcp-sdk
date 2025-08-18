package com.mcp.sdk;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Abstract base class for MCP resources.
 * Provides common functionality for resource registration, validation, and error handling.
 */
public abstract class MCPResource extends AbstractVerticle {

    private ToolEventLogger eventLogger = ToolEventLogger.NOOP;

    /**
     * Get the resource URI that this resource handles
     * @return the resource URI (e.g., "file:///path/to/resource", "custom://resource-name")
     */
    protected abstract String getResourceUri();

    /**
     * Get the resource definition metadata
     * @return JsonObject containing resource metadata (name, description, mimeType, etc.)
     */
    protected abstract JsonObject getResourceDefinition();

    /**
     * Read the resource content
     * @param uri the resource URI to read
     * @return JsonObject containing the resource content and metadata
     * @throws Exception if resource reading fails
     */
    protected abstract JsonObject readResource(String uri) throws Exception;

    /**
     * Validate the resource URI (optional override)
     * @param uri the URI to validate
     * @throws IllegalArgumentException if URI is invalid
     */
    protected void validateUri(String uri) throws IllegalArgumentException {
        if (uri == null || uri.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource URI cannot be null or empty");
        }
    }

    /**
     * Called after resource is initialized (optional override)
     */
    protected void onResourceInitialized() {
        // Default implementation does nothing
    }

    /**
     * Set the event logger for this resource
     * @param logger the event logger instance
     */
    public void setEventLogger(ToolEventLogger logger) {
        this.eventLogger = logger != null ? logger : ToolEventLogger.NOOP;
    }

    /**
     * Get the event logger instance
     * @return the event logger
     */
    protected ToolEventLogger getEventLogger() {
        return eventLogger;
    }

    @Override
    public void start() throws Exception {
        String resourceUri = getResourceUri();
        String eventBusAddress = "resources." + resourceUri.replaceAll("[^a-zA-Z0-9]", "_");
        
        // Register event bus handler for this resource
        vertx.eventBus().consumer(eventBusAddress, this::handleMessage);
        
        // Call resource initialization hook
        onResourceInitialized();
        
        eventLogger.logToolExecution(resourceUri, "initialize", "success", 
            new JsonObject().put("eventBusAddress", eventBusAddress), 
            "Resource initialized successfully", null);
        

    }

    /**
     * Handle event bus messages for this resource
     */
    protected final void handleMessage(Message<JsonObject> message) {
        String resourceUri = getResourceUri();
        JsonObject request = message.body();
        String uri = request.getString("uri");
        
        try {
            eventLogger.logToolExecution(resourceUri, "start", "processing", 
                new JsonObject().put("uri", uri), null, null);
            
            // Validate URI
            validateUri(uri);
            eventLogger.logToolExecution(resourceUri, "validation", "success", 
                new JsonObject().put("uri", uri), null, null);
            
            // Read resource
            JsonObject result = readResource(uri);
            eventLogger.logToolExecution(resourceUri, "read", "success", 
                new JsonObject().put("uri", uri), "Resource read successfully", null);
            
            message.reply(result);
            
        } catch (IllegalArgumentException e) {
            // Validation error
            eventLogger.logToolExecution(resourceUri, "validation", "failed", 
                new JsonObject().put("uri", uri), e.getMessage(), null);
            JsonObject error = createErrorResponse(-32602, e.getMessage());
            message.reply(error);
            
        } catch (Exception e) {
            // Read error
            eventLogger.logToolExecution(resourceUri, "read", "exception", 
                new JsonObject().put("uri", uri), "Error: " + e.getMessage(), null);
            JsonObject error = createErrorResponse(-32603, resourceUri + " read failed: " + e.getMessage());
            message.reply(error);
        }
    }

    /**
     * Create a success response
     * @param content the resource content
     * @param mimeType the MIME type of the content
     * @return JsonObject containing the success response
     */
    protected JsonObject createSuccessResponse(String content, String mimeType) {
        return new JsonObject()
            .put("contents", new JsonArray()
                .add(new JsonObject()
                    .put("uri", getResourceUri())
                    .put("mimeType", mimeType != null ? mimeType : "text/plain")
                    .put("text", content)));
    }

    /**
     * Create a binary success response
     * @param data the binary data (base64 encoded)
     * @param mimeType the MIME type of the content
     * @return JsonObject containing the success response
     */
    protected JsonObject createBinaryResponse(String data, String mimeType) {
        return new JsonObject()
            .put("contents", new JsonArray()
                .add(new JsonObject()
                    .put("uri", getResourceUri())
                    .put("mimeType", mimeType != null ? mimeType : "application/octet-stream")
                    .put("blob", data)));
    }

    /**
     * Create an error response
     * @param code the error code
     * @param message the error message
     * @return JsonObject containing the error response
     */
    protected JsonObject createErrorResponse(int code, String message) {
        return new JsonObject()
            .put("contents", new JsonArray()
                .add(new JsonObject()
                    .put("type", "text")
                    .put("text", "Error: " + message)));
    }

    /**
     * Get a required string parameter from URI or throw exception
     * @param uri the URI to parse
     * @param paramName the parameter name to extract
     * @return the parameter value
     * @throws IllegalArgumentException if parameter is missing
     */
    protected String getRequiredUriParam(String uri, String paramName) throws IllegalArgumentException {
        // Simple parameter extraction - can be enhanced for more complex URIs
        if (uri.contains(paramName + "=")) {
            String[] parts = uri.split(paramName + "=");
            if (parts.length > 1) {
                String value = parts[1].split("&")[0];
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        throw new IllegalArgumentException("Required parameter '" + paramName + "' not found in URI: " + uri);
    }

    /**
     * Get an optional string parameter from URI
     * @param uri the URI to parse
     * @param paramName the parameter name to extract
     * @param defaultValue the default value if parameter is not found
     * @return the parameter value or default value
     */
    protected String getOptionalUriParam(String uri, String paramName, String defaultValue) {
        try {
            return getRequiredUriParam(uri, paramName);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
} 