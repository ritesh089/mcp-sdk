package com.mcp.sdk;

import com.mcp.sdk.annotations.ToolMethod;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Support class providing default implementations for MCPToolHandler.
 * 
 * This class provides utility methods that make it easier to implement
 * the MCPToolHandler interface, particularly the executeToolMethod method
 * which can be complex to implement manually.
 */
public class MCPToolHandlerSupport {
    
    /**
     * Default implementation of executeToolMethod that uses reflection
     * to find and invoke the appropriate method.
     * 
     * This is a convenience method that tools can use to implement
     * executeToolMethod without having to write reflection code.
     * 
     * @param handler the tool handler instance
     * @param methodName the name of the method to execute
     * @param parameters the method parameters as a map
     * @param context the MCP context
     * @return the result of the method execution
     * @throws Exception if the method execution fails
     */
    public static Object executeToolMethodByReflection(
            MCPToolHandler handler, 
            String methodName, 
            Map<String, Object> parameters, 
            MCPContext context) throws Exception {
        
        Class<?> handlerClass = handler.getToolClass();
        Object instance = handler.getToolInstance();
        
        // Find the method
        Method method = findToolMethod(handlerClass, methodName);
        if (method == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }
        
        // Convert parameters map to ordered array
        Object[] args = buildParameterArray(method, parameters, context);
        
        // Invoke the method
        method.setAccessible(true);
        return method.invoke(instance, args);
    }
    
    /**
     * Find a tool method by name in the given class.
     */
    private static Method findToolMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && 
                (method.isAnnotationPresent(ToolMethod.class) || hasParameterAnnotations(method))) {
                return method;
            }
        }
        return null;
    }
    
    /**
     * Check if a method has parameter annotations.
     */
    private static boolean hasParameterAnnotations(Method method) {
        return java.util.Arrays.stream(method.getParameters())
            .anyMatch(p -> p.isAnnotationPresent(com.mcp.sdk.annotations.Parameter.class));
    }
    
    /**
     * Build parameter array from parameter map and method signature.
     */
    private static Object[] buildParameterArray(Method method, Map<String, Object> parameters, MCPContext context) {
        java.lang.reflect.Parameter[] methodParams = method.getParameters();
        Object[] args = new Object[methodParams.length];
        
        for (int i = 0; i < methodParams.length; i++) {
            java.lang.reflect.Parameter param = methodParams[i];
            
            // Check if this parameter is MCPContext
            if (param.getType().equals(MCPContext.class)) {
                args[i] = context;
                continue;
            }
            
            // Get parameter name
            String paramName = getParameterName(param);
            args[i] = parameters.get(paramName);
        }
        
        return args;
    }
    
    /**
     * Get parameter name from method parameter.
     */
    private static String getParameterName(java.lang.reflect.Parameter parameter) {
        com.mcp.sdk.annotations.Parameter annotation = 
            parameter.getAnnotation(com.mcp.sdk.annotations.Parameter.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        return parameter.getName();
    }
}
