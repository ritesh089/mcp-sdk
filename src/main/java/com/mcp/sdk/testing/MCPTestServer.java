package com.mcp.sdk.testing;

import com.mcp.sdk.MCPPrompt;
import com.mcp.sdk.MCPResource;
import com.mcp.sdk.MCPTool;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Test server for MCP components providing isolated testing environment.
 */
public class MCPTestServer {
    
    private final Vertx vertx;
    private final Map<String, MCPTool> tools = new HashMap<>();
    private final Map<String, MCPResource> resources = new HashMap<>(); 
    private final Map<String, MCPPrompt> prompts = new HashMap<>();
    private boolean started = false;

    private MCPTestServer(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Create a new test server instance.
     */
    public static MCPTestServer create() {
        return new MCPTestServer(Vertx.vertx());
    }

    /**
     * Create a test server with custom Vertx instance.
     */
    public static MCPTestServer create(Vertx vertx) {
        return new MCPTestServer(vertx);
    }

    /**
     * Add a tool to the test server.
     */
    public MCPTestServer withTool(MCPTool tool) {
        String toolName = extractToolName(tool);
        tools.put(toolName, tool);
        return this;
    }

    /**
     * Add a resource to the test server.
     */
    public MCPTestServer withResource(MCPResource resource) {
        String resourceUri = extractResourceUri(resource);
        resources.put(resourceUri, resource);
        return this;
    }

    /**
     * Add a prompt to the test server.
     */
    public MCPTestServer withPrompt(MCPPrompt prompt) {
        String promptName = extractPromptName(prompt);
        prompts.put(promptName, prompt);
        return this;
    }

    /**
     * Start the test server and deploy all components.
     */
    public MCPTestServer start() {
        if (started) {
            throw new IllegalStateException("Test server is already started");
        }

        try {
            // Deploy all tools
            for (MCPTool tool : tools.values()) {
                CompletableFuture<String> deployFuture = new CompletableFuture<>();
                vertx.deployVerticle(tool).onComplete(ar -> {
                    if (ar.succeeded()) {
                        deployFuture.complete(ar.result());
                    } else {
                        deployFuture.completeExceptionally(ar.cause());
                    }
                });
                deployFuture.get(5, TimeUnit.SECONDS);
            }

            // Deploy all resources
            for (MCPResource resource : resources.values()) {
                CompletableFuture<String> deployFuture = new CompletableFuture<>();
                vertx.deployVerticle(resource).onComplete(ar -> {
                    if (ar.succeeded()) {
                        deployFuture.complete(ar.result());
                    } else {
                        deployFuture.completeExceptionally(ar.cause());
                    }
                });
                deployFuture.get(5, TimeUnit.SECONDS);
            }

            // Deploy all prompts
            for (MCPPrompt prompt : prompts.values()) {
                CompletableFuture<String> deployFuture = new CompletableFuture<>();
                vertx.deployVerticle(prompt).onComplete(ar -> {
                    if (ar.succeeded()) {
                        deployFuture.complete(ar.result());
                    } else {
                        deployFuture.completeExceptionally(ar.cause());
                    }
                });
                deployFuture.get(5, TimeUnit.SECONDS);
            }

            started = true;
            return this;

        } catch (Exception e) {
            throw new RuntimeException("Failed to start test server", e);
        }
    }

    /**
     * Call a tool with the specified arguments.
     */
    public TestResult callTool(String toolName, JsonObject arguments) {
        ensureStarted();
        
        MCPTool tool = tools.get(toolName);
        if (tool == null) {
            return TestResult.error("Tool not found: " + toolName);
        }

        try {
            CompletableFuture<JsonObject> resultFuture = new CompletableFuture<>();
            
            JsonObject request = new JsonObject().put("arguments", arguments);
            String eventBusAddress = "tools." + toolName;
            
            vertx.eventBus().<JsonObject>request(eventBusAddress, request).onComplete(ar -> {
                if (ar.succeeded()) {
                    resultFuture.complete(ar.result().body());
                } else {
                    resultFuture.completeExceptionally(ar.cause());
                }
            });

            JsonObject result = resultFuture.get(10, TimeUnit.SECONDS);
            return TestResult.success(result);

        } catch (Exception e) {
            return TestResult.error("Tool execution failed: " + e.getMessage());
        }
    }

    /**
     * Read a resource with the specified URI.
     */
    public TestResult readResource(String uri) {
        ensureStarted();
        
        MCPResource resource = resources.get(uri);
        if (resource == null) {
            return TestResult.error("Resource not found: " + uri);
        }

        try {
            CompletableFuture<JsonObject> resultFuture = new CompletableFuture<>();
            
            JsonObject request = new JsonObject().put("uri", uri);
            String eventBusAddress = "resources." + uri.replaceAll("[^a-zA-Z0-9]", "_");
            
            vertx.eventBus().<JsonObject>request(eventBusAddress, request).onComplete(ar -> {
                if (ar.succeeded()) {
                    resultFuture.complete(ar.result().body());
                } else {
                    resultFuture.completeExceptionally(ar.cause());
                }
            });

            JsonObject result = resultFuture.get(10, TimeUnit.SECONDS);
            return TestResult.success(result);

        } catch (Exception e) {
            return TestResult.error("Resource read failed: " + e.getMessage());
        }
    }

    /**
     * Execute a prompt with the specified arguments.
     */
    public TestResult executePrompt(String promptName, JsonObject arguments) {
        ensureStarted();
        
        MCPPrompt prompt = prompts.get(promptName);
        if (prompt == null) {
            return TestResult.error("Prompt not found: " + promptName);
        }

        try {
            CompletableFuture<JsonObject> resultFuture = new CompletableFuture<>();
            
            JsonObject request = new JsonObject()
                .put("name", promptName)
                .put("arguments", arguments);
            String eventBusAddress = "prompt." + promptName;
            
            vertx.eventBus().<JsonObject>request(eventBusAddress, request).onComplete(ar -> {
                if (ar.succeeded()) {
                    resultFuture.complete(ar.result().body());
                } else {
                    resultFuture.completeExceptionally(ar.cause());
                }
            });

            JsonObject result = resultFuture.get(10, TimeUnit.SECONDS);
            return TestResult.success(result);

        } catch (Exception e) {
            return TestResult.error("Prompt execution failed: " + e.getMessage());
        }
    }

    /**
     * List all available tools.
     */
    public JsonArray listTools() {
        JsonArray toolList = new JsonArray();
        tools.forEach((name, tool) -> {
            try {
                JsonObject toolDef = new JsonObject()
                    .put("name", name)
                    .put("description", extractToolDescription(tool));
                toolList.add(toolDef);
            } catch (Exception e) {
                // Skip tools that can't provide definition
            }
        });
        return toolList;
    }

    /**
     * List all available resources.
     */
    public JsonArray listResources() {
        JsonArray resourceList = new JsonArray();
        resources.forEach((uri, resource) -> {
            try {
                JsonObject resourceDef = new JsonObject()
                    .put("uri", uri)
                    .put("name", extractResourceName(resource))
                    .put("description", extractResourceDescription(resource));
                resourceList.add(resourceDef);
            } catch (Exception e) {
                // Skip resources that can't provide definition
            }
        });
        return resourceList;
    }

    /**
     * List all available prompts.
     */
    public JsonArray listPrompts() {
        JsonArray promptList = new JsonArray();
        prompts.forEach((name, prompt) -> {
            try {
                JsonObject promptDef = new JsonObject()
                    .put("name", name)
                    .put("description", extractPromptDescription(prompt));
                promptList.add(promptDef);
            } catch (Exception e) {
                // Skip prompts that can't provide definition
            }
        });
        return promptList;
    }

    /**
     * Stop the test server and cleanup resources.
     */
    public void stop() {
        if (!started) return;
        
        try {
            CompletableFuture<Void> closeFuture = new CompletableFuture<>();
            vertx.close().onComplete(ar -> {
                if (ar.succeeded()) {
                    closeFuture.complete(null);
                } else {
                    closeFuture.completeExceptionally(ar.cause());
                }
            });
            closeFuture.get(5, TimeUnit.SECONDS);
            started = false;
        } catch (Exception e) {
            System.err.println("Error stopping test server: " + e.getMessage());
        }
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("Test server is not started. Call start() first.");
        }
    }

    // Helper methods to extract component information using reflection
    private String extractToolName(MCPTool tool) {
        try {
            return (String) tool.getClass().getMethod("getToolName").invoke(tool);
        } catch (Exception e) {
            return tool.getClass().getSimpleName().toLowerCase().replace("tool", "");
        }
    }

    private String extractToolDescription(MCPTool tool) {
        try {
            return (String) tool.getClass().getMethod("getToolDescription").invoke(tool);
        } catch (Exception e) {
            return "No description available";
        }
    }

    private String extractResourceUri(MCPResource resource) {
        try {
            return (String) resource.getClass().getMethod("getResourceUri").invoke(resource);
        } catch (Exception e) {
            return "unknown://" + resource.getClass().getSimpleName();
        }
    }

    private String extractResourceName(MCPResource resource) {
        try {
            return (String) resource.getClass().getMethod("getResourceName").invoke(resource);
        } catch (Exception e) {
            return resource.getClass().getSimpleName();
        }
    }

    private String extractResourceDescription(MCPResource resource) {
        try {
            return (String) resource.getClass().getMethod("getResourceDescription").invoke(resource);
        } catch (Exception e) {
            return "No description available";
        }
    }

    private String extractPromptName(MCPPrompt prompt) {
        try {
            return (String) prompt.getClass().getMethod("getPromptName").invoke(prompt);
        } catch (Exception e) {
            return prompt.getClass().getSimpleName().toLowerCase().replace("prompt", "");
        }
    }

    private String extractPromptDescription(MCPPrompt prompt) {
        try {
            return (String) prompt.getClass().getMethod("getPromptDescription").invoke(prompt);
        } catch (Exception e) {
            return "No description available";
        }
    }
}