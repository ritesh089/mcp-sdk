package com.mcp.sdk.exceptions;

/**
 * Exception thrown when a requested tool is not found.
 */
public class MCPToolNotFoundException extends MCPException {
    
    public MCPToolNotFoundException(String toolName) {
        super(MCPErrorCodes.TOOL_NOT_FOUND, "Tool not found: " + toolName);
    }
    
    public MCPToolNotFoundException(String toolName, Throwable cause) {
        super(MCPErrorCodes.TOOL_NOT_FOUND, "Tool not found: " + toolName, cause);
    }
}