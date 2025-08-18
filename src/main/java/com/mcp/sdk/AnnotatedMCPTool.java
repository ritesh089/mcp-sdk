package com.mcp.sdk;

import com.mcp.sdk.annotations.Parameter;
import com.mcp.sdk.exceptions.MCPValidationException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Enhanced MCP Tool base class with annotation support and type-safe parameter binding.
 * Automatically generates tool definitions from annotations and method signatures.
 */
public abstract class AnnotatedMCPTool extends com.mcp.sdk.MCPTool {

    private final Map<String, Method> toolMethods = new HashMap<>();
    private final Map<String, JsonObject> cachedDefinitions = new HashMap<>();

    @Override
    protected void onToolInitialized() {
        // Initialize annotation processing during tool initialization
        initializeAnnotationProcessing();
        super.onToolInitialized();
    }

    /**
     * Initialize annotation processing and method discovery.
     */
    private void initializeAnnotationProcessing() {
        String toolName = getToolName();
        
        // Generate tool definition (this also populates toolMethods)
        generateToolDefinition(toolName);
        
        // Ensure we found a tool method
        if (!toolMethods.containsKey(toolName)) {
            throw new IllegalStateException("No valid tool method found in " + getClass().getSimpleName() + 
                ". Methods must be annotated with @ToolMethod or have parameters annotated with @Parameter.");
        }
    }

    @Override
    protected final String getToolName() {
        com.mcp.sdk.annotations.MCPTool annotation = getClass().getAnnotation(com.mcp.sdk.annotations.MCPTool.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        return getClass().getSimpleName().toLowerCase().replace("tool", "");
    }

    /**
     * Get the tool description from annotation.
     */
    protected final String getToolDescription() {
        com.mcp.sdk.annotations.MCPTool annotation = getClass().getAnnotation(com.mcp.sdk.annotations.MCPTool.class);
        if (annotation != null) {
            return annotation.description();
        }
        throw new IllegalStateException("@MCPTool annotation with description is required");
    }

    /**
     * Get the tool definition generated from annotations.
     */
    protected final JsonObject getToolDefinition() {
        String toolName = getToolName();
        return cachedDefinitions.computeIfAbsent(toolName, this::generateToolDefinition);
    }

    private JsonObject generateToolDefinition(String toolName) {
        
        JsonObject definition = new JsonObject()
            .put("type", "object")
            .put("properties", new JsonObject())
            .put("required", new JsonArray());

        JsonObject properties = definition.getJsonObject("properties");
        JsonArray required = definition.getJsonArray("required");

        // Find the main tool method (should be public and not inherited from base class)
        Method toolMethod = findToolMethod();
        if (toolMethod != null) {
            toolMethods.put(toolName, toolMethod);
            
            // Process method parameters
            java.lang.reflect.Parameter[] parameters = toolMethod.getParameters();
            for (java.lang.reflect.Parameter param : parameters) {
                Parameter paramAnnotation = param.getAnnotation(Parameter.class);
                if (paramAnnotation != null) {
                    String paramName = paramAnnotation.name().isEmpty() ? param.getName() : paramAnnotation.name();
                    
                    JsonObject paramDef = new JsonObject()
                        .put("description", paramAnnotation.description());
                    
                    // Add type information
                    addTypeInfo(paramDef, param.getType());
                    
                    // Add validation constraints
                    addValidationConstraints(paramDef, paramAnnotation);
                    
                    properties.put(paramName, paramDef);
                    
                    if (paramAnnotation.required()) {
                        required.add(paramName);
                    }
                }
            }
        }

        return new JsonObject()
            .put("name", toolName)
            .put("description", getToolDescription())
            .put("inputSchema", definition);
    }

    private Method findToolMethod() {
        // Look for public methods that aren't from the base class
        return Arrays.stream(getClass().getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(com.mcp.sdk.annotations.ToolMethod.class) ||
                            (method.getParameterCount() > 0 && 
                             Arrays.stream(method.getParameters()).anyMatch(p -> p.isAnnotationPresent(Parameter.class))))
            .findFirst()
            .orElse(null);
    }

    private void addTypeInfo(JsonObject paramDef, Class<?> type) {
        if (type == String.class) {
            paramDef.put("type", "string");
        } else if (type == int.class || type == Integer.class) {
            paramDef.put("type", "integer");
        } else if (type == long.class || type == Long.class) {
            paramDef.put("type", "integer");
        } else if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            paramDef.put("type", "number");
        } else if (type == boolean.class || type == Boolean.class) {
            paramDef.put("type", "boolean");
        } else if (type.isArray()) {
            paramDef.put("type", "array");
        } else {
            paramDef.put("type", "object");
        }
    }

    private void addValidationConstraints(JsonObject paramDef, Parameter annotation) {
        if (annotation.min() != Double.NEGATIVE_INFINITY) {
            paramDef.put("minimum", annotation.min());
        }
        if (annotation.max() != Double.POSITIVE_INFINITY) {
            paramDef.put("maximum", annotation.max());
        }
        if (annotation.enumValues().length > 0) {
            paramDef.put("enum", new JsonArray(Arrays.asList(annotation.enumValues())));
        }
        if (!annotation.pattern().isEmpty()) {
            paramDef.put("pattern", annotation.pattern());
        }
        if (!annotation.defaultValue().isEmpty()) {
            paramDef.put("default", annotation.defaultValue());
        }
    }

    @Override
    protected final JsonObject handleToolCall(JsonObject arguments) throws Exception {
        // Legacy method - delegate to context-aware version
        MCPContext emptyContext = DefaultMCPContext.builder().build();
        return handleToolCall(arguments, emptyContext);
    }

    private Object extractAndValidateParameter(String paramName, Class<?> type, Parameter annotation, JsonObject arguments) {
        Object value = arguments.getValue(paramName);

        // Handle required parameters
        if (value == null) {
            if (annotation.required()) {
                throw new MCPValidationException("Required parameter '" + paramName + "' is missing");
            } else if (!annotation.defaultValue().isEmpty()) {
                value = parseDefaultValue(annotation.defaultValue(), type);
            } else {
                return getDefaultValue(type);
            }
        }

        // Convert and validate the value
        Object convertedValue = convertValue(value, type);
        validateValue(paramName, convertedValue, annotation);
        
        return convertedValue;
    }

    private Object parseDefaultValue(String defaultValue, Class<?> type) {
        if (type == String.class) return defaultValue;
        if (type == int.class || type == Integer.class) return Integer.parseInt(defaultValue);
        if (type == long.class || type == Long.class) return Long.parseLong(defaultValue);
        if (type == double.class || type == Double.class) return Double.parseDouble(defaultValue);
        if (type == float.class || type == Float.class) return Float.parseFloat(defaultValue);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(defaultValue);
        return defaultValue;
    }

    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == boolean.class) return false;
        return null;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;

        if (targetType == String.class) {
            return value.toString();
        } else if (targetType == int.class || targetType == Integer.class) {
            return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString());
        } else if (targetType == long.class || targetType == Long.class) {
            return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
        } else if (targetType == double.class || targetType == Double.class) {
            return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
        } else if (targetType == float.class || targetType == Float.class) {
            return value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(value.toString());
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());
        }

        return value;
    }

    private void validateValue(String paramName, Object value, Parameter annotation) {
        if (value == null) return;

        // Numeric range validation
        if (value instanceof Number) {
            double numValue = ((Number) value).doubleValue();
            if (annotation.min() != Double.NEGATIVE_INFINITY && numValue < annotation.min()) {
                throw new MCPValidationException("Parameter '" + paramName + "' value " + numValue + " is below minimum " + annotation.min());
            }
            if (annotation.max() != Double.POSITIVE_INFINITY && numValue > annotation.max()) {
                throw new MCPValidationException("Parameter '" + paramName + "' value " + numValue + " exceeds maximum " + annotation.max());
            }
        }

        // Enum validation
        if (annotation.enumValues().length > 0) {
            String strValue = value.toString();
            boolean validEnum = Arrays.asList(annotation.enumValues()).contains(strValue);
            if (!validEnum) {
                throw new MCPValidationException("Parameter '" + paramName + "' value '" + strValue + 
                    "' is not one of: " + Arrays.toString(annotation.enumValues()));
            }
        }

        // Pattern validation
        if (!annotation.pattern().isEmpty() && value instanceof String) {
            String strValue = (String) value;
            if (!Pattern.matches(annotation.pattern(), strValue)) {
                throw new MCPValidationException("Parameter '" + paramName + "' value '" + strValue + 
                    "' does not match pattern: " + annotation.pattern());
            }
        }
    }

    @Override
    protected JsonObject handleToolCall(JsonObject arguments, MCPContext context) throws Exception {
        String toolName = getToolName();
        Method toolMethod = toolMethods.get(toolName);
        
        if (toolMethod == null) {
            throw new MCPValidationException("No tool method found for: " + toolName);
        }
        
        try {
            // Extract and validate parameters with context support
            Object[] parameters = extractParameters(toolMethod, arguments, context);
            
            // Invoke the tool method
            Object result = toolMethod.invoke(this, parameters);
            
            // Convert result to JsonObject
            return convertResult(result);
        } catch (Exception e) {
            if (e.getCause() instanceof MCPValidationException) {
                throw (MCPValidationException) e.getCause();
            }
            throw new RuntimeException("Failed to execute tool method: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract parameters from arguments, with support for MCPContext injection
     */
    private Object[] extractParameters(Method toolMethod, JsonObject arguments, MCPContext context) {
        if (toolMethod == null) {
            return new Object[0];
        }
        
        java.lang.reflect.Parameter[] methodParams = toolMethod.getParameters();
        Object[] result = new Object[methodParams.length];
        
        for (int i = 0; i < methodParams.length; i++) {
            java.lang.reflect.Parameter param = methodParams[i];
            
            // Check if this parameter is MCPContext
            if (param.getType().equals(MCPContext.class)) {
                result[i] = context;
                continue;
            }
            
            // Handle regular @Parameter annotated parameters
            Parameter annotation = param.getAnnotation(Parameter.class);
            if (annotation != null) {
                String paramName = annotation.name().isEmpty() ? param.getName() : annotation.name();
                Object value = extractAndValidateParameter(paramName, param.getType(), annotation, arguments);
                result[i] = value;
            } else {
                // Parameter without annotation - try to get by name
                result[i] = arguments.getValue(param.getName());
            }
        }
        
        return result;
    }

    private JsonObject convertResult(Object result) {
        if (result == null) {
            return createSuccessResponse("Operation completed successfully");
        }
        
        if (result instanceof JsonObject) {
            return (JsonObject) result;
        }
        
        if (result instanceof String) {
            return createSuccessResponse((String) result);
        }
        
        if (result instanceof ToolResult) {
            ToolResult toolResult = (ToolResult) result;
            return toolResult.toJsonObject();
        }
        
        // For other types, convert to string representation
        return createSuccessResponse("Result: " + result.toString());
    }
}