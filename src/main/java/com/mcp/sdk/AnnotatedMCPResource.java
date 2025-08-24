package com.mcp.sdk;

import com.mcp.sdk.annotations.MCPResource;

import io.vertx.core.json.JsonObject;

/**
 * Enhanced base class for MCP resources that use annotation-based configuration.
 * Provides automatic metadata extraction from @MCPResource annotation.
 */
public abstract class AnnotatedMCPResource extends com.mcp.sdk.MCPResource {

    private MCPResource annotation;

    @Override
    public void start() throws Exception {
        // Extract annotation metadata
        this.annotation = this.getClass().getAnnotation(MCPResource.class);
        if (annotation == null) {
            throw new IllegalStateException("AnnotatedMCPResource must be annotated with @MCPResource");
        }
        super.start();
    }

    @Override
    protected String getResourceUri() {
        return annotation.uri();
    }

    @Override
    protected JsonObject getResourceDefinition() {
        return new JsonObject()
            .put("uri", annotation.uri())
            .put("name", annotation.name())
            .put("description", annotation.description())
            .put("mimeType", annotation.mimeType());
    }

    /**
     * Subclasses must implement this method to provide the actual resource content.
     * This method should throw MCPResourceNotFoundException for invalid URIs.
     */
    @Override
    protected abstract JsonObject readResource(String uri) throws Exception;
}