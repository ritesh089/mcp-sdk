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
    protected String getToolName() {
        Class<?> annotationSource = getToolAnnotationSource();
        com.mcp.sdk.annotations.MCPTool annotation = annotationSource.getAnnotation(com.mcp.sdk.annotations.MCPTool.class);
        if (annotation != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        return getClass().getSimpleName().toLowerCase().replace("tool", "");
    }

    /**
     * Get the tool description from annotation.
     */
    protected String getToolDescription() {
        Class<?> annotationSource = getToolAnnotationSource();
        com.mcp.sdk.annotations.MCPTool annotation = annotationSource.getAnnotation(com.mcp.sdk.annotations.MCPTool.class);
        if (annotation != null) {
            return annotation.description();
        }
        throw new IllegalStateException("@MCPTool annotation with description is required");
    }

    /**
     * Get the tool definition generated from annotations.
     */
    protected JsonObject getToolDefinition() {
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
        Class<?> annotationSource = getToolAnnotationSource();
        return Arrays.stream(annotationSource.getDeclaredMethods())
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
        } else if (targetType.isArray()) {
            // Handle array types
            return convertToArray(value, targetType);
        }

        return value;
    }
    
    private Object convertToArray(Object value, Class<?> arrayType) {
        if (value == null) return null;
        
        Class<?> componentType = arrayType.getComponentType();
        
        // Handle JsonArray from Vert.x
        if (value instanceof io.vertx.core.json.JsonArray) {
            io.vertx.core.json.JsonArray jsonArray = (io.vertx.core.json.JsonArray) value;
            
            if (componentType == String.class) {
                String[] result = new String[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    Object item = jsonArray.getValue(i);
                    result[i] = item != null ? item.toString() : null;
                }
                return result;
            } else if (componentType == int.class || componentType == Integer.class) {
                int[] result = new int[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    Integer intValue = jsonArray.getInteger(i);
                    result[i] = intValue != null ? intValue : 0;
                }
                return result;
            } else if (componentType == double.class || componentType == Double.class) {
                double[] result = new double[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    Double doubleValue = jsonArray.getDouble(i);
                    result[i] = doubleValue != null ? doubleValue : 0.0;
                }
                return result;
            }
        }
        
        // Handle Java arrays or lists
        if (value instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) value;
            if (componentType == String.class) {
                return list.stream().map(Object::toString).toArray(String[]::new);
            }
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
            
            // Invoke the tool method using the extensible execution method
            Object result = executeToolMethod(toolMethod.getName(), parameters, context);
            
            // Use ResponseProcessor to convert any return type to ToolResult
            ToolResult toolResult = ResponseProcessor.processResponse(result, toolMethod);
            return toolResult.toJsonObject();
            
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            
            // Handle MCPToolException with rich error information
            if (cause instanceof MCPToolException) {
                ToolResult errorResult = ResponseProcessor.processException((MCPToolException) cause, toolMethod);
                return errorResult.toJsonObject();
            }
            
            // Handle MCPValidationException (legacy)
            if (cause instanceof MCPValidationException) {
                throw (MCPValidationException) cause;
            }
            
            // Handle any other exception
            ToolResult errorResult = ResponseProcessor.processGenericException((Exception) cause, toolMethod);
            return errorResult.toJsonObject();
            
        } catch (MCPValidationException e) {
            // Direct validation exception
            throw e;
        } catch (Exception e) {
            // Any other exception during parameter extraction or method invocation
            ToolResult errorResult = ResponseProcessor.processGenericException(e, toolMethod);
            return errorResult.toJsonObject();
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

    /**
     * Get the class to scan for tool annotations.
     * Override this to provide a different class for annotation discovery.
     * Used by adapters to delegate annotation discovery to wrapped objects.
     */
    protected Class<?> getToolAnnotationSource() {
        return this.getClass();
    }
    
    /**
     * Execute a tool method with the given parameters.
     * Override this to customize method execution behavior.
     * Used by adapters to delegate method execution to wrapped objects.
     */
    protected Object executeToolMethod(String methodName, Object[] parameters, MCPContext context) throws Exception {
        Method method = getToolMethod(methodName);
        if (method == null) {
            throw new IllegalArgumentException("Method not found: " + methodName);
        }
        
        Object instance = getToolInstance();
        return method.invoke(instance, parameters);
    }
    
    /**
     * Get the tool method by name.
     * Override this to customize method resolution.
     */
    protected Method getToolMethod(String methodName) {
        return toolMethods.get(methodName);
    }
    
    /**
     * Get the instance to invoke methods on.
     * Override this to customize instance resolution.
     */
    protected Object getToolInstance() {
        return this;
    }

}