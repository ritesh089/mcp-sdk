package com.mcp.sdk.content;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Map;

/**
 * Content item for structured data (POJOs, Maps, Collections, etc.).
 * Automatically serializes the data to JSON format.
 */
public class DataContent extends ContentItem {
    private final Object data;
    private final String dataType;

    public DataContent(Object data) {
        super("data");
        this.data = data;
        this.dataType = data != null ? data.getClass().getSimpleName() : "null";
        this.metadata.put("dataType", dataType);
    }

    public DataContent(Object data, Map<String, Object> metadata) {
        super("data", metadata);
        this.data = data;
        this.dataType = data != null ? data.getClass().getSimpleName() : "null";
        this.metadata.put("dataType", dataType);
    }

    public Object getData() {
        return data;
    }

    public String getDataType() {
        return dataType;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject()
            .put("type", type)
            .put("dataType", dataType);

        // Serialize the data based on its type
        if (data == null) {
            json.putNull("data");
        } else if (data instanceof JsonObject) {
            json.put("data", (JsonObject) data);
        } else if (data instanceof JsonArray) {
            json.put("data", (JsonArray) data);
        } else {
            // Use Vert.x JSON serialization for POJOs and other objects
            try {
                json.put("data", JsonObject.mapFrom(data));
            } catch (Exception e) {
                // Fallback to string representation if serialization fails
                json.put("data", data.toString());
                json.put("serializationError", e.getMessage());
            }
        }

        if (!metadata.isEmpty()) {
            json.put("metadata", new JsonObject(metadata));
        }

        return json;
    }

    @Override
    public String toString() {
        return String.format("DataContent{dataType='%s', metadata=%s}", 
                           dataType, metadata);
    }
}
