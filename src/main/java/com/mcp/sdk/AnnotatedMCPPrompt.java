package com.mcp.sdk;

import com.mcp.sdk.annotations.MCPPrompt;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Enhanced base class for MCP prompts that use annotation-based configuration.
 * Provides automatic metadata extraction from @MCPPrompt annotation.
 */
public abstract class AnnotatedMCPPrompt extends com.mcp.sdk.MCPPrompt {

    private MCPPrompt annotation;

    @Override
    public void start() throws Exception {
        // Extract annotation metadata
        this.annotation = this.getClass().getAnnotation(MCPPrompt.class);
        if (annotation == null) {
            throw new IllegalStateException("AnnotatedMCPPrompt must be annotated with @MCPPrompt");
        }
        super.start();
    }

    @Override
    protected String getPromptName() {
        return annotation.name();
    }

    @Override
    protected String getPromptDescription() {
        return annotation.description();
    }

    @Override
    protected JsonArray getPromptArguments() {
        JsonArray args = new JsonArray();
        for (String arg : annotation.arguments()) {
            // Parse argument definition in format "name:type:description"
            String[] parts = arg.split(":", 3);
            if (parts.length >= 2) {
                JsonObject argDef = new JsonObject()
                    .put("name", parts[0])
                    .put("type", parts[1]);
                if (parts.length == 3) {
                    argDef.put("description", parts[2]);
                }
                args.add(argDef);
            }
        }
        return args;
    }

    /**
     * Subclasses must implement this method to generate the actual prompt content.
     */
    @Override
    protected abstract JsonObject generatePrompt(JsonObject arguments);
}