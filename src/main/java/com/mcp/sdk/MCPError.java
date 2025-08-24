package com.mcp.sdk;

import java.util.*;

/**
 * Rich error information for MCP tools that provides meaningful context to LLMs.
 * Supports structured error messages with suggestions, context, and recovery guidance.
 */
public class MCPError {
    private final String errorType;
    private final String userMessage;
    private final String technicalMessage;
    private final String suggestedAction;
    private final Map<String, Object> context;
    private final List<String> suggestions;
    private final int errorCode;

    private MCPError(String errorType, int errorCode, String userMessage, String technicalMessage,
                     String suggestedAction, Map<String, Object> context, List<String> suggestions) {
        this.errorType = errorType;
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.technicalMessage = technicalMessage;
        this.suggestedAction = suggestedAction;
        this.context = context != null ? new HashMap<>(context) : new HashMap<>();
        this.suggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
    }

    // Static factory methods for common error types
    public static MCPErrorBuilder validation(String message) {
        return new MCPErrorBuilder("validation", -32602, message);
    }

    public static MCPErrorBuilder business(String message) {
        return new MCPErrorBuilder("business", -32001, message);
    }

    public static MCPErrorBuilder system(String message) {
        return new MCPErrorBuilder("system", -32603, message);
    }

    public static MCPErrorBuilder permission(String message) {
        return new MCPErrorBuilder("permission", -32000, message);
    }

    public static MCPErrorBuilder custom(String errorType, int errorCode, String message) {
        return new MCPErrorBuilder(errorType, errorCode, message);
    }

    // Getters
    public String getErrorType() { return errorType; }
    public String getUserMessage() { return userMessage; }
    public String getTechnicalMessage() { return technicalMessage; }
    public String getSuggestedAction() { return suggestedAction; }
    public Map<String, Object> getContext() { return new HashMap<>(context); }
    public List<String> getSuggestions() { return new ArrayList<>(suggestions); }
    public int getErrorCode() { return errorCode; }

    /**
     * Builder class for constructing rich MCPError instances with fluent API.
     */
    public static class MCPErrorBuilder {
        private final String errorType;
        private final int errorCode;
        private final String userMessage;
        private String technicalMessage;
        private String suggestedAction;
        private Map<String, Object> context = new HashMap<>();
        private List<String> suggestions = new ArrayList<>();

        private MCPErrorBuilder(String errorType, int errorCode, String userMessage) {
            this.errorType = errorType;
            this.errorCode = errorCode;
            this.userMessage = userMessage;
        }

        public MCPErrorBuilder withTechnicalDetails(String details) {
            this.technicalMessage = details;
            return this;
        }

        public MCPErrorBuilder withSuggestedAction(String action) {
            this.suggestedAction = action;
            return this;
        }

        public MCPErrorBuilder withContext(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public MCPErrorBuilder withSuggestions(String... suggestions) {
            this.suggestions.addAll(Arrays.asList(suggestions));
            return this;
        }

        public MCPErrorBuilder withSuggestions(List<String> suggestions) {
            this.suggestions.addAll(suggestions);
            return this;
        }

        public MCPErrorBuilder withErrorCode(int code) {
            // Note: This creates a new builder with different error code
            return new MCPErrorBuilder(this.errorType, code, this.userMessage)
                .withTechnicalDetails(this.technicalMessage)
                .withSuggestedAction(this.suggestedAction)
                .withSuggestions(this.suggestions)
                .withContext(this.context);
        }

        public MCPErrorBuilder withContext(Map<String, Object> context) {
            this.context.putAll(context);
            return this;
        }

        public MCPError build() {
            return new MCPError(errorType, errorCode, userMessage, technicalMessage,
                              suggestedAction, context, suggestions);
        }
    }

    @Override
    public String toString() {
        return String.format("MCPError{type='%s', code=%d, message='%s'}", 
                           errorType, errorCode, userMessage);
    }
}
