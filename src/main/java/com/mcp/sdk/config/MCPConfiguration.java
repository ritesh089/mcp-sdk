package com.mcp.sdk.config;

import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration container for MCP components.
 */
public class MCPConfiguration {
    
    private final Map<String, ToolConfiguration> tools = new HashMap<>();
    private final Map<String, ResourceConfiguration> resources = new HashMap<>();
    private final Map<String, PromptConfiguration> prompts = new HashMap<>();

    public void addTool(ToolConfiguration tool) {
        tools.put(tool.getName(), tool);
    }

    public void addResource(ResourceConfiguration resource) {
        resources.put(resource.getUri(), resource);
    }

    public void addPrompt(PromptConfiguration prompt) {
        prompts.put(prompt.getName(), prompt);
    }

    public Map<String, ToolConfiguration> getTools() {
        return new HashMap<>(tools);
    }

    public Map<String, ResourceConfiguration> getResources() {
        return new HashMap<>(resources);
    }

    public Map<String, PromptConfiguration> getPrompts() {
        return new HashMap<>(prompts);
    }

    /**
     * Check if this configuration has any components.
     */
    public boolean hasComponents() {
        return !tools.isEmpty() || !resources.isEmpty() || !prompts.isEmpty();
    }

    /**
     * Export configuration to JSON format for file generation.
     */
    public JsonObject toJson() {
        JsonObject config = new JsonObject();
        
        JsonObject toolsJson = new JsonObject();
        tools.forEach((name, tool) -> toolsJson.put(name, tool.toJson()));
        config.put("tools", toolsJson);
        
        JsonObject resourcesJson = new JsonObject();
        resources.forEach((uri, resource) -> resourcesJson.put(uri, resource.toJson()));
        config.put("resources", resourcesJson);
        
        JsonObject promptsJson = new JsonObject();
        prompts.forEach((name, prompt) -> promptsJson.put(name, prompt.toJson()));
        config.put("prompts", promptsJson);
        
        return config;
    }
}

/**
 * Configuration for a tool component.
 */
class ToolConfiguration {
    private final String name;
    private final String className;
    private final String eventBusAddress;
    private final JsonObject definition;

    public ToolConfiguration(String name, String className, String eventBusAddress, JsonObject definition) {
        this.name = name;
        this.className = className;
        this.eventBusAddress = eventBusAddress;
        this.definition = definition;
    }

    public String getName() { return name; }
    public String getClassName() { return className; }
    public String getEventBusAddress() { return eventBusAddress; }
    public JsonObject getDefinition() { return definition; }

    public JsonObject toJson() {
        return new JsonObject()
            .put("name", name)
            .put("className", className)
            .put("eventBusAddress", eventBusAddress)
            .put("definition", definition);
    }
}

/**
 * Configuration for a resource component.
 */
class ResourceConfiguration {
    private final String uri;
    private final String className;
    private final String eventBusAddress;
    private final JsonObject definition;

    public ResourceConfiguration(String uri, String className, String eventBusAddress, JsonObject definition) {
        this.uri = uri;
        this.className = className;
        this.eventBusAddress = eventBusAddress;
        this.definition = definition;
    }

    public String getUri() { return uri; }
    public String getClassName() { return className; }
    public String getEventBusAddress() { return eventBusAddress; }
    public JsonObject getDefinition() { return definition; }

    public JsonObject toJson() {
        return new JsonObject()
            .put("uri", uri)
            .put("className", className)
            .put("eventBusAddress", eventBusAddress)
            .put("definition", definition);
    }
}

/**
 * Configuration for a prompt component.
 */
class PromptConfiguration {
    private final String name;
    private final String className;
    private final String eventBusAddress;
    private final JsonObject definition;

    public PromptConfiguration(String name, String className, String eventBusAddress, JsonObject definition) {
        this.name = name;
        this.className = className;
        this.eventBusAddress = eventBusAddress;
        this.definition = definition;
    }

    public String getName() { return name; }
    public String getClassName() { return className; }
    public String getEventBusAddress() { return eventBusAddress; }
    public JsonObject getDefinition() { return definition; }

    public JsonObject toJson() {
        return new JsonObject()
            .put("name", name)
            .put("className", className)
            .put("eventBusAddress", eventBusAddress)
            .put("definition", definition);
    }
}