package com.mcp.sdk;

import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of MCPContext.
 * Thread-safe and immutable for safe sharing across async operations.
 */
public class DefaultMCPContext implements MCPContext {
    
    private final String correlationId;
    private final String mcpId;
    private final String sessionId;
    private final String method;
    private final Object requestId;
    private final Map<String, Object> attributes;
    private final JsonObject metadata;
    private final long startTime;
    private final String clientAddress;
    private final boolean streaming;
    
    private DefaultMCPContext(Builder builder) {
        this.correlationId = builder.correlationId;
        this.mcpId = builder.mcpId;
        this.sessionId = builder.sessionId;
        this.method = builder.method;
        this.requestId = builder.requestId;
        this.attributes = new ConcurrentHashMap<>(builder.attributes);
        this.metadata = builder.metadata != null ? builder.metadata.copy() : new JsonObject();
        this.startTime = builder.startTime;
        this.clientAddress = builder.clientAddress;
        this.streaming = builder.streaming;
    }
    
    @Override
    public Optional<String> getCorrelationId() {
        return Optional.ofNullable(correlationId);
    }
    
    @Override
    public Optional<String> getMcpId() {
        return Optional.ofNullable(mcpId);
    }
    
    @Override
    public Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId);
    }
    
    @Override
    public Optional<String> getMethod() {
        return Optional.ofNullable(method);
    }
    
    @Override
    public Optional<Object> getRequestId() {
        return Optional.ofNullable(requestId);
    }
    
    @Override
    public Optional<Object> getAttribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }
    
    @Override
    public JsonObject getMetadata() {
        return metadata.copy();
    }
    
    @Override
    public long getStartTime() {
        return startTime;
    }
    
    @Override
    public Optional<String> getClientAddress() {
        return Optional.ofNullable(clientAddress);
    }
    
    @Override
    public boolean isStreaming() {
        return streaming;
    }
    
    @Override
    public MCPContext withAttributes(Map<String, Object> additionalAttributes) {
        return builder()
            .correlationId(correlationId)
            .mcpId(mcpId)
            .sessionId(sessionId)
            .method(method)
            .requestId(requestId)
            .metadata(metadata)
            .startTime(startTime)
            .clientAddress(clientAddress)
            .streaming(streaming)
            .attributes(attributes)
            .attributes(additionalAttributes)
            .build();
    }
    
    @Override
    public MCPContext withAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>();
        newAttributes.put(key, value);
        return withAttributes(newAttributes);
    }
    
    /**
     * Create a new builder for constructing MCPContext instances.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating MCPContext instances.
     */
    public static class Builder {
        private String correlationId;
        private String mcpId;
        private String sessionId;
        private String method;
        private Object requestId;
        private Map<String, Object> attributes = new HashMap<>();
        private JsonObject metadata = new JsonObject();
        private long startTime = System.currentTimeMillis();
        private String clientAddress;
        private boolean streaming = false;
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder mcpId(String mcpId) {
            this.mcpId = mcpId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder method(String method) {
            this.method = method;
            return this;
        }
        
        public Builder requestId(Object requestId) {
            this.requestId = requestId;
            return this;
        }
        
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }
        
        public Builder metadata(JsonObject metadata) {
            this.metadata = metadata != null ? metadata.copy() : new JsonObject();
            return this;
        }
        
        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder clientAddress(String clientAddress) {
            this.clientAddress = clientAddress;
            return this;
        }
        
        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }
        
        public DefaultMCPContext build() {
            return new DefaultMCPContext(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format("MCPContext{correlationId='%s', mcpId='%s', method='%s', requestId=%s}", 
                           correlationId, mcpId, method, requestId);
    }
}
