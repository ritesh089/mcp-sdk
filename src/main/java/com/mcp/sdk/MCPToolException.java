package com.mcp.sdk;

import java.util.*;

/**
 * Exception class for MCP tools that provides rich error information to LLMs.
 * This exception carries structured error data that can be converted to meaningful
 * error responses for LLM consumption.
 */
public class MCPToolException extends Exception {
    private final MCPError mcpError;

    private MCPToolException(MCPError mcpError) {
        super(mcpError.getUserMessage());
        this.mcpError = mcpError;
    }

    private MCPToolException(MCPError mcpError, Throwable cause) {
        super(mcpError.getUserMessage(), cause);
        this.mcpError = mcpError;
    }

    // Static factory methods for common error types
    public static MCPToolException validation(String message) {
        MCPError error = MCPError.validation(message).build();
        return new MCPToolException(error);
    }

    public static MCPToolException business(String message) {
        MCPError error = MCPError.business(message).build();
        return new MCPToolException(error);
    }

    public static MCPToolException system(String message) {
        MCPError error = MCPError.system(message).build();
        return new MCPToolException(error);
    }

    public static MCPToolException permission(String message) {
        MCPError error = MCPError.permission(message).build();
        return new MCPToolException(error);
    }

    public static MCPToolException custom(String errorType, int errorCode, String message) {
        MCPError error = MCPError.custom(errorType, errorCode, message).build();
        return new MCPToolException(error);
    }

    // Builder-style methods for fluent API
    public MCPToolException withTechnicalDetails(String details) {
        MCPError.MCPErrorBuilder builder = rebuildError().withTechnicalDetails(details);
        return new MCPToolException(builder.build(), this.getCause());
    }

    public MCPToolException withSuggestedAction(String action) {
        MCPError.MCPErrorBuilder builder = rebuildError().withSuggestedAction(action);
        return new MCPToolException(builder.build(), this.getCause());
    }

    public MCPToolException withContext(String key, Object value) {
        MCPError.MCPErrorBuilder builder = rebuildError().withContext(key, value);
        return new MCPToolException(builder.build(), this.getCause());
    }

    public MCPToolException withSuggestions(String... suggestions) {
        MCPError.MCPErrorBuilder builder = rebuildError().withSuggestions(suggestions);
        return new MCPToolException(builder.build(), this.getCause());
    }

    public MCPToolException withSuggestions(List<String> suggestions) {
        MCPError.MCPErrorBuilder builder = rebuildError().withSuggestions(suggestions);
        return new MCPToolException(builder.build(), this.getCause());
    }

    public MCPToolException withCause(Throwable cause) {
        return new MCPToolException(this.mcpError, cause);
    }

    private MCPError.MCPErrorBuilder rebuildError() {
        return MCPError.custom(mcpError.getErrorType(), mcpError.getErrorCode(), mcpError.getUserMessage())
            .withTechnicalDetails(mcpError.getTechnicalMessage())
            .withSuggestedAction(mcpError.getSuggestedAction())
            .withSuggestions(mcpError.getSuggestions())
            .withContext(mcpError.getContext());
    }

    // Convenience methods for common patterns
    public static MCPToolException validationWithSuggestions(String message, String... suggestions) {
        return validation(message).withSuggestions(suggestions);
    }

    public static MCPToolException businessWithAction(String message, String suggestedAction) {
        return business(message).withSuggestedAction(suggestedAction);
    }

    public static MCPToolException systemWithCause(String message, Throwable cause) {
        return system(message).withCause(cause).withTechnicalDetails(cause.getMessage());
    }

    // Getters
    public MCPError getMcpError() {
        return mcpError;
    }

    public String getErrorType() {
        return mcpError.getErrorType();
    }

    public String getUserMessage() {
        return mcpError.getUserMessage();
    }

    public String getTechnicalMessage() {
        return mcpError.getTechnicalMessage();
    }

    public String getSuggestedAction() {
        return mcpError.getSuggestedAction();
    }

    public Map<String, Object> getContext() {
        return mcpError.getContext();
    }

    public List<String> getSuggestions() {
        return mcpError.getSuggestions();
    }

    public int getErrorCode() {
        return mcpError.getErrorCode();
    }

    @Override
    public String toString() {
        return String.format("MCPToolException{%s}", mcpError.toString());
    }
}
