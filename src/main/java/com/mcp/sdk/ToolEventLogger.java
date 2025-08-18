package com.mcp.sdk;

import io.vertx.core.json.JsonObject;

/**
 * Lightweight event logger interface for MCP tools.
 * This allows tools to log events without depending on the server framework.
 * The actual implementation is provided by the server framework.
 */
public interface ToolEventLogger {
    
    /**
     * Log a tool execution event
     * @param toolName the name of the tool
     * @param phase the execution phase (start, validation, execution, complete, etc.)
     * @param status the status (processing, success, failed, exception)
     * @param arguments the tool arguments (can be null)
     * @param message optional message (can be null)
     * @param error optional error details (can be null)
     */
    void logToolExecution(String toolName, String phase, String status, 
                         JsonObject arguments, String message, String error);
    
    /**
     * No-op implementation for when no logger is available
     */
    ToolEventLogger NOOP = new ToolEventLogger() {
        @Override
        public void logToolExecution(String toolName, String phase, String status, 
                                   JsonObject arguments, String message, String error) {
            // No operation
        }
    };
} 