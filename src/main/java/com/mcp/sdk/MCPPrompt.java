package com.mcp.sdk;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Abstract base class for MCP prompts.
 * Provides common functionality for prompt registration, validation, and error handling.
 */
public abstract class MCPPrompt extends AbstractVerticle {

    private ToolEventLogger eventLogger = ToolEventLogger.NOOP;

    /**
     * Get the prompt name that this prompt handles
     * @return the prompt name (e.g., "code-review", "explain-code")
     */
    protected abstract String getPromptName();

    /**
     * Get the prompt description
     * @return a human-readable description of what this prompt does
     */
    protected abstract String getPromptDescription();

    /**
     * Get the arguments definition for this prompt
     * @return JsonArray of argument definitions with name, description, and required fields
     */
    protected abstract JsonArray getPromptArguments();

    /**
     * Generate the prompt content based on the provided arguments
     * @param arguments the arguments provided by the client
     * @return JsonObject containing the prompt result with description and messages
     */
    protected abstract JsonObject generatePrompt(JsonObject arguments);

    /**
     * Validate prompt arguments (override for custom validation)
     * @param arguments the arguments to validate
     * @throws IllegalArgumentException if validation fails
     */
    protected void validateArguments(JsonObject arguments) {
        JsonArray argumentDefs = getPromptArguments();
        
        for (int i = 0; i < argumentDefs.size(); i++) {
            JsonObject argDef = argumentDefs.getJsonObject(i);
            String argName = argDef.getString("name");
            boolean required = argDef.getBoolean("required", false);
            
            if (required && !arguments.containsKey(argName)) {
                throw new IllegalArgumentException("Required argument '" + argName + "' is missing");
            }
        }
    }

    /**
     * Set the event logger for this prompt
     * @param logger the event logger to use
     */
    public void setEventLogger(ToolEventLogger logger) {
        this.eventLogger = logger != null ? logger : ToolEventLogger.NOOP;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        try {
            String promptName = getPromptName();
            String eventBusAddress = "prompt." + promptName;
            
            eventLogger.logToolExecution(promptName, "startup", "registering", 
                new JsonObject().put("eventBusAddress", eventBusAddress), null, null);
            
            // Register event bus consumer for this prompt
            vertx.eventBus().consumer(eventBusAddress, this::handleMessage);
            
            eventLogger.logToolExecution(promptName, "startup", "success", 
                new JsonObject().put("eventBusAddress", eventBusAddress), 
                "Prompt registered successfully", null);
            
            System.err.println("MCPPrompt started: " + promptName + " listening on " + eventBusAddress);
            startPromise.complete();
            
        } catch (Exception e) {
            eventLogger.logToolExecution(getPromptName(), "startup", "failed", 
                new JsonObject(), "Startup failed: " + e.getMessage(), null);
            startPromise.fail(e);
        }
    }

    protected void handleMessage(Message<JsonObject> message) {
        String promptName = getPromptName();
        JsonObject request = message.body();
        JsonObject arguments = request.getJsonObject("arguments", new JsonObject());
        
        try {
            eventLogger.logToolExecution(promptName, "start", "processing", arguments, null, null);
            
            // Validate arguments
            validateArguments(arguments);
            eventLogger.logToolExecution(promptName, "validation", "success", arguments, null, null);
            
            // Generate prompt
            JsonObject response = generatePrompt(arguments);
            
            // Ensure response has proper format
            if (response == null) {
                response = createSuccessResponse("Prompt generated successfully", new JsonArray());
            }
            
            eventLogger.logToolExecution(promptName, "execution", "success", arguments, 
                "Prompt generation completed", null);
            
            message.reply(response);
            
        } catch (IllegalArgumentException e) {
            // Validation error
            eventLogger.logToolExecution(promptName, "validation", "failed", arguments, 
                e.getMessage(), null);
            JsonObject error = createErrorResponse(-32602, e.getMessage());
            message.reply(error);
            
        } catch (Exception e) {
            // Execution error
            eventLogger.logToolExecution(promptName, "execution", "exception", arguments, 
                "Error: " + e.getMessage(), null);
            JsonObject error = createErrorResponse(-32603, promptName + " failed: " + e.getMessage());
            message.reply(error);
        }
    }

    /**
     * Create a standard success response for prompts
     * @param description description of the prompt
     * @param messages array of messages for the prompt
     * @return formatted JsonObject response
     */
    protected JsonObject createSuccessResponse(String description, JsonArray messages) {
        return new JsonObject()
            .put("description", description)
            .put("messages", messages);
    }

    /**
     * Create a standard error response
     * @param code the error code
     * @param message the error message
     * @return formatted JsonObject error response
     */
    protected JsonObject createErrorResponse(int code, String message) {
        return new JsonObject()
            .put("content", new JsonArray()
                .add(new JsonObject()
                    .put("type", "text")
                    .put("text", "Error: " + message)));
    }

    /**
     * Create a user message for a prompt
     * @param text the message text
     * @return JsonObject representing a user message
     */
    protected JsonObject createUserMessage(String text) {
        return new JsonObject()
            .put("role", "user")
            .put("content", new JsonObject()
                .put("type", "text")
                .put("text", text));
    }

    /**
     * Create an assistant message for a prompt
     * @param text the message text
     * @return JsonObject representing an assistant message
     */
    protected JsonObject createAssistantMessage(String text) {
        return new JsonObject()
            .put("role", "assistant")
            .put("content", new JsonObject()
                .put("type", "text")
                .put("text", text));
    }

    /**
     * Utility method to get a required string parameter
     * @param arguments the arguments object
     * @param paramName the parameter name
     * @return the parameter value
     * @throws IllegalArgumentException if parameter is missing or not a string
     */
    protected String getRequiredString(JsonObject arguments, String paramName) {
        String value = arguments.getString(paramName);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' is required");
        }
        return value;
    }

    /**
     * Utility method to get an optional string parameter
     * @param arguments the arguments object
     * @param paramName the parameter name
     * @param defaultValue the default value if parameter is missing
     * @return the parameter value or default
     */
    protected String getOptionalString(JsonObject arguments, String paramName, String defaultValue) {
        return arguments.getString(paramName, defaultValue);
    }
}