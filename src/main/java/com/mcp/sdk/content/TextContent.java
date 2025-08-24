package com.mcp.sdk.content;

import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * Content item for plain text content.
 */
public class TextContent extends ContentItem {
    private final String text;

    public TextContent(String text) {
        super("text");
        this.text = text != null ? text : "";
    }

    public TextContent(String text, Map<String, Object> metadata) {
        super("text", metadata);
        this.text = text != null ? text : "";
    }

    public String getText() {
        return text;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject()
            .put("type", type)
            .put("text", text);

        if (!metadata.isEmpty()) {
            json.put("metadata", new JsonObject(metadata));
        }

        return json;
    }

    @Override
    public String toString() {
        return String.format("TextContent{text='%s', metadata=%s}", 
                           text.length() > 50 ? text.substring(0, 50) + "..." : text, 
                           metadata);
    }
}
