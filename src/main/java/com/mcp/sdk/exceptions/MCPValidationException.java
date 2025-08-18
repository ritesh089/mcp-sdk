package com.mcp.sdk.exceptions;

/**
 * Exception thrown when parameter validation fails.
 */
public class MCPValidationException extends MCPException {
    
    public MCPValidationException(String message) {
        super(MCPErrorCodes.VALIDATION_ERROR, message);
    }
    
    public MCPValidationException(String message, Throwable cause) {
        super(MCPErrorCodes.VALIDATION_ERROR, message, cause);
    }
}