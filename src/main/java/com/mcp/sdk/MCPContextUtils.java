package com.mcp.sdk;

import org.slf4j.MDC;

/**
 * Utility class for working with MCPContext in tool implementations.
 * Provides convenience methods for context management and logging enhancement.
 */
public class MCPContextUtils {
    
    /**
     * Apply the MCPContext to the current thread's MDC.
     * This ensures that logging statements automatically include context information.
     * 
     * @param context the MCP context to apply
     */
    public static void applyToMDC(MCPContext context) {
        if (context == null) {
            return;
        }
        
        context.getCorrelationId().ifPresent(id -> MDC.put("correlationId", id));
        context.getMcpId().ifPresent(id -> MDC.put("mcpId", id));
        context.getSessionId().ifPresent(id -> MDC.put("sessionId", id));
        context.getMethod().ifPresent(method -> MDC.put("method", method));
        
        if (context.getRequestId().isPresent()) {
            MDC.put("requestId", String.valueOf(context.getRequestId().get()));
        }
        
        context.getClientAddress().ifPresent(addr -> MDC.put("clientAddress", addr));
        MDC.put("streaming", String.valueOf(context.isStreaming()));
    }
    
    /**
     * Clear MCPContext from the current thread's MDC.
     */
    public static void clearMDC() {
        MDC.remove("correlationId");
        MDC.remove("mcpId");
        MDC.remove("sessionId");
        MDC.remove("method");
        MDC.remove("requestId");
        MDC.remove("clientAddress");
        MDC.remove("streaming");
    }
    
    /**
     * Execute a task with MCPContext applied to MDC.
     * Automatically cleans up the MDC after execution.
     * 
     * @param context the MCP context to apply
     * @param task the task to execute
     */
    public static void withContext(MCPContext context, Runnable task) {
        try {
            applyToMDC(context);
            task.run();
        } finally {
            clearMDC();
        }
    }
    
    /**
     * Get a formatted string representation of key context information.
     * Useful for logging or debugging.
     * 
     * @param context the MCP context
     * @return formatted context string
     */
    public static String formatContext(MCPContext context) {
        if (context == null) {
            return "MCPContext{null}";
        }
        
        StringBuilder sb = new StringBuilder("MCPContext{");
        
        context.getCorrelationId().ifPresent(id -> sb.append("correlationId='").append(id).append("', "));
        context.getMcpId().ifPresent(id -> sb.append("mcpId='").append(id).append("', "));
        context.getSessionId().ifPresent(id -> sb.append("sessionId='").append(id).append("', "));
        context.getMethod().ifPresent(method -> sb.append("method='").append(method).append("', "));
        context.getRequestId().ifPresent(id -> sb.append("requestId=").append(id).append(", "));
        
        long duration = System.currentTimeMillis() - context.getStartTime();
        sb.append("duration=").append(duration).append("ms");
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Create a metrics object from the context for performance tracking.
     * 
     * @param context the MCP context
     * @param additionalData any additional metrics data
     * @return metrics object suitable for logging
     */
    public static io.vertx.core.json.JsonObject createMetrics(MCPContext context, String... additionalData) {
        io.vertx.core.json.JsonObject metrics = new io.vertx.core.json.JsonObject();
        
        if (context != null) {
            context.getCorrelationId().ifPresent(id -> metrics.put("correlationId", id));
            context.getMcpId().ifPresent(id -> metrics.put("mcpId", id));
            context.getSessionId().ifPresent(id -> metrics.put("sessionId", id));
            context.getMethod().ifPresent(method -> metrics.put("method", method));
            
            long duration = System.currentTimeMillis() - context.getStartTime();
            metrics.put("durationMs", duration);
            metrics.put("startTime", context.getStartTime());
            metrics.put("streaming", context.isStreaming());
        }
        
        // Add additional data as key-value pairs
        for (int i = 0; i < additionalData.length; i += 2) {
            if (i + 1 < additionalData.length) {
                metrics.put(additionalData[i], additionalData[i + 1]);
            }
        }
        
        return metrics;
    }
}
