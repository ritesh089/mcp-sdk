package com.mcp.sdk.content;

import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for different types of content that can be included in MCP tool responses.
 * Each content item has a type and optional metadata.
 */
public abstract class ContentItem {
    protected final String type;
    protected final Map<String, Object> metadata;

    protected ContentItem(String type) {
        this.type = type;
        this.metadata = new HashMap<>();
    }

    protected ContentItem(String type, Map<String, Object> metadata) {
        this.type = type;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public ContentItem withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Convert this content item to a JSON object for MCP protocol transmission.
     * @return JsonObject representation of this content item
     */
    public abstract JsonObject toJson();

    @Override
    public String toString() {
        return String.format("%s{type='%s', metadata=%s}", 
                           getClass().getSimpleName(), type, metadata);
    }
}
