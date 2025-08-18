package com.mcp.sdk.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 */
public class MCPResourceNotFoundException extends MCPException {
    
    public MCPResourceNotFoundException(String resourceUri) {
        super(MCPErrorCodes.RESOURCE_NOT_FOUND, "Resource not found: " + resourceUri);
    }
    
    public MCPResourceNotFoundException(String resourceUri, Throwable cause) {
        super(MCPErrorCodes.RESOURCE_NOT_FOUND, "Resource not found: " + resourceUri, cause);
    }
}