package com.mcp.sdk;

import com.mcp.sdk.annotations.MCPTool;
import java.util.Map;

/**
 * Interface for MCP Tool business logic implementations.
 * 
 * This interface allows tools to implement pure business logic without extending
 * framework classes, promoting clean architecture and easier testing.
 * 
 * Tools implementing this interface can maintain the same method signatures
 * and annotations (@ToolMethod, @Parameter) while being completely decoupled
 * from the framework implementation.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @MCPTool(name = "calculator", description = "Calculator tool")
 * public class CalculatorTool implements MCPToolHandler {
 *     
 *     @ToolMethod
 *     public CalculationResult calculate(
 *         @Parameter(name = "expression") String expression,
 *         MCPContext context) {
 *         // Pure business logic here
 *         return new CalculationResult(expression, evaluate(expression));
 *     }
 *     
 *     // ✅ NO BOILERPLATE METHODS NEEDED!
 *     // Interface provides defaults that read from @MCPTool annotation
 * }
 * }
 * </pre>
 * 
 * The framework will automatically detect implementations of this interface
 * and wrap them with the appropriate adapter for deployment.
 */
public interface MCPToolHandler {
    
    /**
     * Get the name of this tool.
     * 
     * Default implementation automatically reads from the @MCPTool annotation,
     * eliminating the need for boilerplate code. Override only if you need
     * custom logic or dynamic tool names.
     * 
     * @return the tool name
     */
    default String getToolName() {
        MCPTool annotation = this.getClass().getAnnotation(MCPTool.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        // Fallback: derive from class name (e.g., "CalculatorTool" -> "calculator")
        return this.getClass().getSimpleName().toLowerCase().replaceAll("tool$", "");
    }
    
    /**
     * Get the description of this tool.
     * 
     * Default implementation automatically reads from the @MCPTool annotation,
     * eliminating the need for boilerplate code. Override only if you need
     * custom logic or dynamic descriptions.
     * 
     * @return the tool description
     */
    default String getToolDescription() {
        MCPTool annotation = this.getClass().getAnnotation(MCPTool.class);
        if (annotation != null) {
            return annotation.description();
        }
        // Fallback description
        return "Tool implementation of " + this.getClass().getSimpleName();
    }
    
    /**
     * Execute a tool method with the given parameters.
     * 
     * This method is called by the framework adapter to invoke the actual
     * business logic. The framework will handle method resolution, parameter
     * binding, and result processing.
     * 
     * The default implementation uses reflection to find and invoke the method.
     * Override this if you need custom method dispatch logic.
     * 
     * @param methodName the name of the method to execute
     * @param parameters the method parameters as a map
     * @param context the MCP context for this execution
     * @return the result of the method execution
     * @throws Exception if the method execution fails
     */
    default Object executeToolMethod(String methodName, Map<String, Object> parameters, MCPContext context) throws Exception {
        return MCPToolHandlerSupport.executeToolMethodByReflection(this, methodName, parameters, context);
    }
    
    /**
     * Get the class that contains the tool annotations and methods.
     * 
     * By default, this returns the implementation class itself, which allows
     * the framework to discover @ToolMethod annotations and other metadata.
     * 
     * @return the class to scan for tool annotations
     */
    default Class<?> getToolClass() {
        return this.getClass();
    }
    
    /**
     * Get the instance that contains the tool methods.
     * 
     * By default, this returns the implementation instance itself.
     * This allows for dependency injection or other instance management patterns.
     * 
     * @return the instance to invoke methods on
     */
    default Object getToolInstance() {
        return this;
    }
    
    /**
     * Validate the tool configuration and method signatures.
     * 
     * This method is called during tool initialization to ensure the tool
     * is properly configured. Override this method to add custom validation logic.
     * 
     * @throws IllegalStateException if the tool configuration is invalid
     */
    default void validateTool() {
        // Default implementation performs basic validation
        if (getToolName() == null || getToolName().trim().isEmpty()) {
            throw new IllegalStateException("Tool name cannot be null or empty");
        }
        if (getToolDescription() == null || getToolDescription().trim().isEmpty()) {
            throw new IllegalStateException("Tool description cannot be null or empty");
        }
        if (getToolClass() == null) {
            throw new IllegalStateException("Tool class cannot be null");
        }
        if (getToolInstance() == null) {
            throw new IllegalStateException("Tool instance cannot be null");
        }
    }
}
