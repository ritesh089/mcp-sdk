package com.mcp.sdk;

import com.mcp.sdk.annotations.Parameter;
import com.mcp.sdk.annotations.ToolMethod;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter that wraps MCPToolHandler implementations to work with the existing
 * AnnotatedMCPTool framework.
 * 
 * This adapter allows tools that implement MCPToolHandler to be deployed
 * using the existing framework infrastructure without requiring inheritance
 * from AnnotatedMCPTool.
 * 
 * The adapter:
 * - Delegates annotation discovery to the handler's class
 * - Delegates method execution to the handler instance
 * - Maintains full compatibility with existing framework features
 * - Preserves all method signatures and parameter binding
 * 
 * Usage:
 * <pre>
 * {@code
 * MCPToolHandler calculator = new CalculatorTool();
 * AnnotatedMCPTool tool = new AnnotatedMCPToolAdapter(calculator);
 * // Deploy tool as usual
 * }
 * </pre>
 */
public class AnnotatedMCPToolAdapter extends AnnotatedMCPTool implements HttpStreamingSupport {
    
    private final MCPToolHandler handler;
    private final Map<String, Method> handlerMethods = new HashMap<>();
    private boolean initialized = false;
    
    /**
     * Create an adapter for the given tool handler.
     * 
     * @param handler the tool handler to adapt
     * @throws IllegalArgumentException if handler is null
     */
    public AnnotatedMCPToolAdapter(MCPToolHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("MCPToolHandler cannot be null");
        }
        this.handler = handler;
    }
    
    /**
     * Get the handler being adapted.
     * 
     * @return the tool handler
     */
    public MCPToolHandler getHandler() {
        return handler;
    }
    
    @Override
    protected void onToolInitialized() {
        // Validate the handler configuration
        handler.validateTool();
        
        // Initialize method discovery from the handler's class
        initializeHandlerMethods();
        
        // Call parent initialization
        super.onToolInitialized();
        
        initialized = true;
    }
    
    /**
     * Discover and cache methods from the handler's class.
     */
    private void initializeHandlerMethods() {
        Class<?> handlerClass = handler.getToolClass();
        
        // Find all methods annotated with @ToolMethod
        for (Method method : handlerClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ToolMethod.class)) {
                method.setAccessible(true);
                handlerMethods.put(method.getName(), method);
            }
        }
        
        // Also look for methods with @Parameter annotations
        for (Method method : handlerClass.getDeclaredMethods()) {
            if (!handlerMethods.containsKey(method.getName())) {
                for (java.lang.reflect.Parameter param : method.getParameters()) {
                    if (param.isAnnotationPresent(Parameter.class)) {
                        method.setAccessible(true);
                        handlerMethods.put(method.getName(), method);
                        break;
                    }
                }
            }
        }
        
        if (handlerMethods.isEmpty()) {
            throw new IllegalStateException(
                "No valid tool methods found in " + handlerClass.getSimpleName() + 
                ". Methods must be annotated with @ToolMethod or have parameters annotated with @Parameter.");
        }
    }
    
    @Override
    protected String getToolName() {
        return handler.getToolName();
    }
    
    @Override
    protected String getToolDescription() {
        return handler.getToolDescription();
    }
    
    /**
     * Override to provide the handler's class for annotation discovery.
     * This allows the framework to discover @MCPTool, @ToolMethod, and @Parameter
     * annotations from the handler's class instead of this adapter class.
     */
    @Override
    protected Class<?> getToolAnnotationSource() {
        return handler.getToolClass();
    }
    
    /**
     * Override method execution to delegate to the handler.
     * This method is called by the parent class after parameter binding.
     */
    @Override
    protected Object executeToolMethod(String methodName, Object[] parameters, MCPContext context) throws Exception {
        Method method = handlerMethods.get(methodName);
        if (method == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }
        
        // Invoke the method directly on the handler instance
        Object instance = handler.getToolInstance();
        return method.invoke(instance, parameters);
    }
    
    /**
     * Extract parameter name from method parameter.
     */
    private String getParameterName(java.lang.reflect.Parameter parameter) {
        Parameter annotation = parameter.getAnnotation(Parameter.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        return parameter.getName();
    }
    
    /**
     * Override to get method from handler's class.
     */
    @Override
    protected Method getToolMethod(String methodName) {
        return handlerMethods.get(methodName);
    }
    
    /**
     * Override to get instance from handler.
     */
    @Override
    protected Object getToolInstance() {
        return handler.getToolInstance();
    }
    
    /**
     * Implement HttpStreamingSupport interface.
     * Delegate streaming calls to the handler if it supports streaming.
     */
    @Override
    public void handleHttpStreaming(JsonObject arguments, Object requestId, String sessionId, String streamingId) {
        if (handler instanceof HttpStreamingSupport) {
            ((HttpStreamingSupport) handler).handleHttpStreaming(arguments, requestId, sessionId, streamingId);
        } else {
            // Handler doesn't support streaming - write error and end stream
            com.mcp.sdk.HttpStreamingHelper.writeJsonRpcError(streamingId, -32601, 
                "Tool " + handler.getToolName() + " does not support HTTP streaming");
            com.mcp.sdk.HttpStreamingHelper.endStream(streamingId);
        }
    }
    
    @Override
    public String toString() {
        return String.format("AnnotatedMCPToolAdapter[handler=%s, tool=%s, streaming=%s]", 
            handler.getClass().getSimpleName(), 
            handler.getToolName(),
            handler instanceof HttpStreamingSupport);
    }
}
