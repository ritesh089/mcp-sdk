package com.mcp.sdk.exceptions;

/**
 * Base exception for MCP-related errors.
 */
public class MCPException extends RuntimeException {
    
    private final int errorCode;
    
    public MCPException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public MCPException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}

/**
 * Standard MCP error codes.
 */
class MCPErrorCodes {
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    
    // Custom error codes (application-specific)
    public static final int RESOURCE_NOT_FOUND = -32001;
    public static final int TOOL_NOT_FOUND = -32002;
    public static final int PROMPT_NOT_FOUND = -32003;
    public static final int VALIDATION_ERROR = -32004;
    public static final int AUTHORIZATION_ERROR = -32005;
    public static final int RATE_LIMIT_ERROR = -32006;
}