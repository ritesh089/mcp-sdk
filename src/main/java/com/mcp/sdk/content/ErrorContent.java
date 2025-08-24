package com.mcp.sdk.content;

import com.mcp.sdk.MCPError;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.util.Map;

/**
 * Content item for error information with rich context for LLMs.
 */
public class ErrorContent extends ContentItem {
    private final MCPError error;

    public ErrorContent(MCPError error) {
        super("error");
        this.error = error;
        this.metadata.put("errorType", error.getErrorType());
        this.metadata.put("errorCode", error.getErrorCode());
    }

    public ErrorContent(MCPError error, Map<String, Object> metadata) {
        super("error", metadata);
        this.error = error;
        this.metadata.put("errorType", error.getErrorType());
        this.metadata.put("errorCode", error.getErrorCode());
    }

    public MCPError getError() {
        return error;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject()
            .put("type", type)
            .put("errorType", error.getErrorType())
            .put("userMessage", error.getUserMessage())
            .put("errorCode", error.getErrorCode());

        if (error.getTechnicalMessage() != null && !error.getTechnicalMessage().isEmpty()) {
            json.put("technicalMessage", error.getTechnicalMessage());
        }

        if (error.getSuggestedAction() != null && !error.getSuggestedAction().isEmpty()) {
            json.put("suggestedAction", error.getSuggestedAction());
        }

        if (!error.getSuggestions().isEmpty()) {
            json.put("suggestions", new JsonArray(error.getSuggestions()));
        }

        if (!error.getContext().isEmpty()) {
            json.put("context", new JsonObject(error.getContext()));
        }

        if (!metadata.isEmpty()) {
            json.put("metadata", new JsonObject(metadata));
        }

        return json;
    }

    @Override
    public String toString() {
        return String.format("ErrorContent{errorType='%s', message='%s', metadata=%s}", 
                           error.getErrorType(), error.getUserMessage(), metadata);
    }
}
