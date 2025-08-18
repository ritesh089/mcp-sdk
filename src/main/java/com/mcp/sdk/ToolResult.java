package com.mcp.sdk;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Represents the result of a tool execution with support for different content types.
 */
public class ToolResult {
    
    private final JsonArray content;
    private final boolean isError;
    private final String errorMessage;
    private final int errorCode;

    private ToolResult(JsonArray content, boolean isError, String errorMessage, int errorCode) {
        this.content = content;
        this.isError = isError;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    /**
     * Create a successful result with text content.
     */
    public static ToolResult success(String text) {
        JsonArray content = new JsonArray().add(
            new JsonObject()
                .put("type", "text")
                .put("text", text)
        );
        return new ToolResult(content, false, null, 0);
    }

    /**
     * Create a successful result with structured content.
     */
    public static ToolResult success(JsonArray content) {
        return new ToolResult(content, false, null, 0);
    }

    /**
     * Create a successful result with mixed content.
     */
    public static ToolResult success(String text, JsonObject data) {
        JsonArray content = new JsonArray()
            .add(new JsonObject().put("type", "text").put("text", text))
            .add(new JsonObject().put("type", "json").put("data", data));
        return new ToolResult(content, false, null, 0);
    }

    /**
     * Create an error result.
     */
    public static ToolResult error(String message) {
        return new ToolResult(null, true, message, -32603);
    }

    /**
     * Create an error result with specific error code.
     */
    public static ToolResult error(int code, String message) {
        return new ToolResult(null, true, message, code);
    }

    /**
     * Convert to JsonObject for MCP response.
     */
    public JsonObject toJsonObject() {
        if (isError) {
            return new JsonObject()
                .put("content", new JsonArray()
                    .add(new JsonObject()
                        .put("type", "text")
                        .put("text", "Error: " + errorMessage)));
        }
        
        return new JsonObject().put("content", content);
    }

    /**
     * Add text content to existing result.
     */
    public ToolResult addText(String text) {
        if (isError) return this;
        
        content.add(new JsonObject()
            .put("type", "text")
            .put("text", text));
        return this;
    }

    /**
     * Add JSON data to existing result.
     */
    public ToolResult addData(JsonObject data) {
        if (isError) return this;
        
        content.add(new JsonObject()
            .put("type", "json")
            .put("data", data));
        return this;
    }

    /**
     * Add image content (base64 encoded).
     */
    public ToolResult addImage(String base64Data, String mimeType) {
        if (isError) return this;
        
        content.add(new JsonObject()
            .put("type", "image")
            .put("data", base64Data)
            .put("mimeType", mimeType));
        return this;
    }

    public boolean isError() {
        return isError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public JsonArray getContent() {
        return content;
    }
}