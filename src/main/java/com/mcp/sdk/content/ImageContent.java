package com.mcp.sdk.content;

import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * Content item for image data (base64 encoded).
 */
public class ImageContent extends ContentItem {
    private final String base64Data;
    private final String mimeType;

    public ImageContent(String base64Data, String mimeType) {
        super("image");
        this.base64Data = base64Data != null ? base64Data : "";
        this.mimeType = mimeType != null ? mimeType : "image/png";
        this.metadata.put("mimeType", this.mimeType);
    }

    public ImageContent(String base64Data, String mimeType, Map<String, Object> metadata) {
        super("image", metadata);
        this.base64Data = base64Data != null ? base64Data : "";
        this.mimeType = mimeType != null ? mimeType : "image/png";
        this.metadata.put("mimeType", this.mimeType);
    }

    public String getBase64Data() {
        return base64Data;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject()
            .put("type", type)
            .put("data", base64Data)
            .put("mimeType", mimeType);

        if (!metadata.isEmpty()) {
            json.put("metadata", new JsonObject(metadata));
        }

        return json;
    }

    @Override
    public String toString() {
        return String.format("ImageContent{mimeType='%s', dataSize=%d, metadata=%s}", 
                           mimeType, base64Data.length(), metadata);
    }
}
