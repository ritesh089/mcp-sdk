package com.mcp.sdk;

import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Optional;

/**
 * Context information passed to MCP tools during execution.
 * Provides access to correlation IDs, tracing data, session information,
 * and other request-scoped metadata.
 */
public interface MCPContext {
    
    /**
     * Get the correlation ID for this request.
     * Used for tracing requests across distributed MCP components.
     * 
     * @return correlation ID if present
     */
    Optional<String> getCorrelationId();
    
    /**
     * Get the MCP ID that is handling this request.
     * 
     * @return MCP ID (e.g., "example-corp:calculator-tools:1.0.0")
     */
    Optional<String> getMcpId();
    
    /**
     * Get the session ID for this request.
     * 
     * @return session ID if present
     */
    Optional<String> getSessionId();
    
    /**
     * Get the MCP method being called.
     * 
     * @return method name (e.g., "tools/call", "resources/read")
     */
    Optional<String> getMethod();
    
    /**
     * Get the JSON-RPC request ID.
     * 
     * @return request ID
     */
    Optional<Object> getRequestId();
    
    /**
     * Get custom attributes set by the framework or other components.
     * 
     * @param key attribute key
     * @return attribute value if present
     */
    Optional<Object> getAttribute(String key);
    
    /**
     * Get all custom attributes.
     * 
     * @return map of all attributes
     */
    Map<String, Object> getAttributes();
    
    /**
     * Get the complete request metadata as JSON.
     * Includes correlation ID, timing, client info, etc.
     * 
     * @return request metadata
     */
    JsonObject getMetadata();
    
    /**
     * Get the timestamp when this request started (in milliseconds).
     * 
     * @return start timestamp
     */
    long getStartTime();
    
    /**
     * Get the client remote address if available.
     * 
     * @return client address
     */
    Optional<String> getClientAddress();
    
    /**
     * Check if this request is part of a streaming operation.
     * 
     * @return true if streaming request
     */
    boolean isStreaming();
    
    /**
     * Create a derived context with additional attributes.
     * Useful for passing enriched context to sub-operations.
     * 
     * @param attributes additional attributes to add
     * @return new context with combined attributes
     */
    MCPContext withAttributes(Map<String, Object> attributes);
    
    /**
     * Create a derived context with a single additional attribute.
     * 
     * @param key attribute key
     * @param value attribute value
     * @return new context with additional attribute
     */
    MCPContext withAttribute(String key, Object value);
}
