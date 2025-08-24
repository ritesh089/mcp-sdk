package com.mcp.sdk;

import com.mcp.sdk.content.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;

/**
 * Enhanced ToolResult class that supports POJO return types, rich error handling,
 * and multiple content types for comprehensive LLM communication.
 */
public class ToolResult {
    private final Object data;
    private final String message;
    private final boolean isError;
    private final MCPError error;
    private final List<ContentItem> content;
    private final Map<String, Object> metadata;

    // Legacy fields for backward compatibility
    private final String errorMessage;
    private final int errorCode;

    private ToolResult(Object data, String message, boolean isError, MCPError error, 
                      List<ContentItem> content, Map<String, Object> metadata) {
        this.data = data;
        this.message = message;
        this.isError = isError;
        this.error = error;
        this.content = content != null ? new ArrayList<>(content) : new ArrayList<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        
        // Legacy compatibility
        this.errorMessage = error != null ? error.getUserMessage() : null;
        this.errorCode = error != null ? error.getErrorCode() : 0;
    }

    // Legacy constructor for backward compatibility
    private ToolResult(JsonArray content, boolean isError, String errorMessage, int errorCode) {
        this.data = null;
        this.message = null;
        this.isError = isError;
        this.error = null;
        this.content = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        
        // Convert legacy JsonArray content to ContentItems
        if (content != null) {
            for (int i = 0; i < content.size(); i++) {
                Object item = content.getValue(i);
                if (item instanceof JsonObject) {
                    JsonObject jsonItem = (JsonObject) item;
                    String type = jsonItem.getString("type", "text");
                    if ("text".equals(type)) {
                        this.content.add(new TextContent(jsonItem.getString("text", "")));
                    } else {
                        this.content.add(new DataContent(jsonItem));
                    }
                } else {
                    this.content.add(new TextContent(item.toString()));
                }
            }
        }
    }

    // ========== NEW POJO-BASED SUCCESS METHODS ==========

    /**
     * Create a successful result with any POJO or primitive type.
     */
    public static <T> ToolResult success(T data) {
        return new ToolResult(data, null, false, null, null, null);
    }

    /**
     * Create a successful result with a message and POJO data.
     */
    public static <T> ToolResult success(String message, T data) {
        return new ToolResult(data, message, false, null, null, null);
    }

    // ========== NEW RICH ERROR METHODS ==========

    /**
     * Create an error result with rich MCPError information.
     */
    public static ToolResult error(MCPError error) {
        return new ToolResult(null, null, true, error, null, null);
    }

    /**
     * Create an error result with MCPToolException.
     */
    public static ToolResult error(MCPToolException exception) {
        return new ToolResult(null, null, true, exception.getMcpError(), null, null);
    }

    // ========== LEGACY METHODS (MAINTAINED FOR BACKWARD COMPATIBILITY) ==========

    /**
     * Create a successful result with text content.
     * @deprecated Use success(String) instead
     */
    @Deprecated
    public static ToolResult success(String text) {
        List<ContentItem> content = Arrays.asList(new TextContent(text));
        return new ToolResult(text, null, false, null, content, null);
    }

    /**
     * Create a successful result with structured content.
     * @deprecated Use success(Object) instead
     */
    @Deprecated
    public static ToolResult success(JsonArray content) {
        return new ToolResult(content, false, null, 0);
    }

    /**
     * Create a successful result with mixed content.
     * @deprecated Use builder() instead
     */
    @Deprecated
    public static ToolResult success(String text, JsonObject data) {
        List<ContentItem> content = Arrays.asList(
            new TextContent(text),
            new DataContent(data)
        );
        return new ToolResult(data, text, false, null, content, null);
    }

    /**
     * Create an error result.
     * @deprecated Use error(MCPError) instead
     */
    @Deprecated
    public static ToolResult error(String message) {
        MCPError error = MCPError.system(message).build();
        return new ToolResult(null, null, true, error, null, null);
    }

    /**
     * Create an error result with specific error code.
     * @deprecated Use error(MCPError) instead
     */
    @Deprecated
    public static ToolResult error(int code, String message) {
        MCPError error = MCPError.custom("system", code, message).build();
        return new ToolResult(null, null, true, error, null, null);
    }

    // ========== BUILDER PATTERN ==========

    /**
     * Create a builder for complex ToolResult construction.
     */
    public static ToolResultBuilder builder() {
        return new ToolResultBuilder();
    }

    public static class ToolResultBuilder {
        private String message;
        private Object data;
        private List<ContentItem> content = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();

        public ToolResultBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ToolResultBuilder data(Object data) {
            this.data = data;
            return this;
        }

        public ToolResultBuilder addText(String text) {
            content.add(new TextContent(text));
            return this;
        }

        public ToolResultBuilder addData(Object data) {
            content.add(new DataContent(data));
            return this;
        }

        public ToolResultBuilder addImage(String base64Data, String mimeType) {
            content.add(new ImageContent(base64Data, mimeType));
            return this;
        }

        public ToolResultBuilder addContent(ContentItem contentItem) {
            content.add(contentItem);
            return this;
        }

        public ToolResultBuilder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public ToolResult build() {
            return new ToolResult(data, message, false, null, content, metadata);
        }
    }

    // ========== LEGACY METHODS (MAINTAINED FOR BACKWARD COMPATIBILITY) ==========

    /**
     * Add text content to existing result.
     * @deprecated Use builder() instead
     */
    @Deprecated
    public ToolResult addText(String text) {
        if (isError) return this;
        content.add(new TextContent(text));
        return this;
    }

    /**
     * Add JSON data to existing result.
     * @deprecated Use builder() instead
     */
    @Deprecated
    public ToolResult addData(JsonObject data) {
        if (isError) return this;
        content.add(new DataContent(data));
        return this;
    }

    /**
     * Add image content (base64 encoded).
     * @deprecated Use builder() instead
     */
    @Deprecated
    public ToolResult addImage(String base64Data, String mimeType) {
        if (isError) return this;
        content.add(new ImageContent(base64Data, mimeType));
        return this;
    }

    // ========== CONVERSION AND OUTPUT METHODS ==========

    /**
     * Convert to JsonObject for MCP response.
     */
    public JsonObject toJsonObject() {
        if (isError) {
            JsonArray contentArray = new JsonArray();
            if (error != null) {
                contentArray.add(new ErrorContent(error).toJson());
            } else {
                // Legacy error handling
                contentArray.add(new JsonObject()
                    .put("type", "text")
                    .put("text", "Error: " + errorMessage));
            }
            
            JsonObject result = new JsonObject().put("content", contentArray);
            if (!metadata.isEmpty()) {
                result.put("metadata", new JsonObject(metadata));
            }
            return result;
        }

        // Build content array
        JsonArray contentArray = new JsonArray();
        for (ContentItem item : content) {
            contentArray.add(item.toJson());
        }

        // If no explicit content but we have data, add it as DataContent
        if (contentArray.isEmpty() && data != null) {
            contentArray.add(new DataContent(data).toJson());
        }

        // Convert DataContent to TextContent for MCP compatibility
        JsonArray compatibleContentArray = new JsonArray();
        for (int i = 0; i < contentArray.size(); i++) {
            JsonObject item = contentArray.getJsonObject(i);
            if ("data".equals(item.getString("type"))) {
                // Convert data content to text content for MCP Inspector compatibility
                String dataType = item.getString("dataType", "Object");
                JsonObject data = item.getJsonObject("data");
                String textContent;
                
                if (data != null) {
                    // Create a readable text representation
                    textContent = String.format("%s: %s", dataType, data.encodePrettily());
                } else {
                    textContent = String.format("%s: %s", dataType, item.getValue("data"));
                }
                
                compatibleContentArray.add(new JsonObject()
                    .put("type", "text")
                    .put("text", textContent));
            } else {
                compatibleContentArray.add(item);
            }
        }
        contentArray = compatibleContentArray;

        // If we have a message but no text content, add it
        if (message != null && !message.isEmpty()) {
            boolean hasTextContent = false;
            for (int i = 0; i < contentArray.size(); i++) {
                if ("text".equals(contentArray.getJsonObject(i).getString("type"))) {
                    hasTextContent = true;
                    break;
                }
            }
            if (!hasTextContent) {
                contentArray.add(0, new TextContent(message).toJson());
            }
        }

        // Ensure we have at least some content
        if (contentArray.isEmpty()) {
            contentArray.add(new TextContent("Operation completed successfully").toJson());
        }

        JsonObject result = new JsonObject().put("content", contentArray);
        if (!metadata.isEmpty()) {
            result.put("metadata", new JsonObject(metadata));
        }
        
        return result;
    }

    // ========== GETTERS ==========

    public Object getData() { return data; }
    public String getMessage() { return message; }
    public boolean isError() { return isError; }
    public MCPError getError() { return error; }
    public List<ContentItem> getContent() { return new ArrayList<>(content); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }

    // Legacy getters for backward compatibility
    public String getErrorMessage() { return errorMessage; }
    public int getErrorCode() { return errorCode; }
    
    /**
     * @deprecated Use getContent() instead
     */
    @Deprecated
    public JsonArray getContent_Legacy() {
        JsonArray legacyContent = new JsonArray();
        for (ContentItem item : content) {
            legacyContent.add(item.toJson());
        }
        return legacyContent;
    }

    @Override
    public String toString() {
        if (isError) {
            return String.format("ToolResult{error=%s}", error != null ? error.toString() : errorMessage);
        }
        return String.format("ToolResult{message='%s', dataType='%s', contentItems=%d, metadata=%s}", 
                           message, data != null ? data.getClass().getSimpleName() : "null", 
                           content.size(), metadata.keySet());
    }
}