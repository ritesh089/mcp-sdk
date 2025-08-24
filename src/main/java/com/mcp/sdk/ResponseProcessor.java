package com.mcp.sdk;

import com.mcp.sdk.annotations.ToolResponse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

/**
 * Processes method return values and converts them to ToolResult instances.
 * Handles POJO serialization, automatic content generation, and response customization.
 */
public class ResponseProcessor {

    /**
     * Process a method's return value and convert it to a ToolResult.
     */
    public static ToolResult processResponse(Object result, Method method) {
        if (result instanceof ToolResult) {
            // Already a ToolResult, return as-is
            return (ToolResult) result;
        }

        ToolResponse config = method.getAnnotation(ToolResponse.class);
        ToolResult.ToolResultBuilder builder = ToolResult.builder();

        // Set success message
        String message = determineMessage(result, method, config);
        if (message != null && !message.isEmpty()) {
            builder.message(message);
        }

        // Process the result based on type
        processResultContent(result, builder, config);

        // Add metadata if enabled
        if (config == null || config.includeMetadata()) {
            addExecutionMetadata(builder, method, result);
        }

        return builder.build();
    }

    /**
     * Process an MCPToolException and convert it to an error ToolResult.
     */
    public static ToolResult processException(MCPToolException exception, Method method) {
        // Add method context to the error
        MCPError.MCPErrorBuilder enhancedErrorBuilder = MCPError.custom(
            exception.getErrorType(), 
            exception.getErrorCode(), 
            exception.getUserMessage())
            .withTechnicalDetails(exception.getTechnicalMessage())
            .withSuggestedAction(exception.getSuggestedAction())
            .withSuggestions(exception.getSuggestions())
            .withContext("methodName", method.getName())
            .withContext("methodClass", method.getDeclaringClass().getSimpleName());

        // Add existing context
        for (Map.Entry<String, Object> entry : exception.getContext().entrySet()) {
            enhancedErrorBuilder.withContext(entry.getKey(), entry.getValue());
        }

        return ToolResult.error(enhancedErrorBuilder.build());
    }

    /**
     * Process a generic exception and convert it to an error ToolResult.
     */
    public static ToolResult processGenericException(Exception exception, Method method) {
        MCPError error = MCPError.system("Unexpected error occurred")
            .withTechnicalDetails(exception.getMessage())
            .withSuggestedAction("Try again or contact support if the problem persists")
            .withContext("methodName", method.getName())
            .withContext("methodClass", method.getDeclaringClass().getSimpleName())
            .withContext("exceptionType", exception.getClass().getSimpleName())
            .build();

        return ToolResult.error(error);
    }

    private static String determineMessage(Object result, Method method, ToolResponse config) {
        if (config != null && !config.message().isEmpty()) {
            return config.message();
        }
        
        return generateDefaultMessage(result, method);
    }

    private static String generateDefaultMessage(Object result, Method method) {
        if (result == null) {
            return method.getName() + " completed successfully";
        }

        String methodName = method.getName();
        String resultType = result.getClass().getSimpleName();

        // Generate contextual messages based on method name patterns
        if (methodName.startsWith("get") || methodName.startsWith("find") || methodName.startsWith("retrieve")) {
            return resultType + " retrieved successfully";
        } else if (methodName.startsWith("create") || methodName.startsWith("add")) {
            return resultType + " created successfully";
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            return resultType + " updated successfully";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "Deletion completed successfully";
        } else if (methodName.startsWith("calculate") || methodName.startsWith("compute")) {
            return "Calculation completed successfully";
        } else if (methodName.startsWith("analyze") || methodName.startsWith("process")) {
            return "Analysis completed successfully";
        } else {
            return "Operation completed successfully";
        }
    }

    private static void processResultContent(Object result, ToolResult.ToolResultBuilder builder, ToolResponse config) {
        if (result == null) {
            builder.addText("Operation completed with no return value");
            return;
        }

        if (result instanceof String) {
            builder.addText((String) result);
            builder.data(result);
        } else if (isPrimitive(result)) {
            builder.addText(result.toString());
            builder.data(result);
        } else if (result instanceof Collection) {
            Collection<?> collection = (Collection<?>) result;
            builder.addText(generateCollectionSummary(collection));
            builder.addData(result);
        } else if (result instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) result;
            builder.addText(generateMapSummary(map));
            builder.addData(result);
        } else {
            // POJO - generate summary and add data
            if (config == null || config.generateSummary()) {
                String summary = generatePOJOSummary(result, config);
                builder.addText(summary);
            }
            builder.addData(result);
        }
    }

    private static boolean isPrimitive(Object obj) {
        return obj instanceof Number || obj instanceof Boolean || obj instanceof Character;
    }

    private static String generateCollectionSummary(Collection<?> collection) {
        if (collection.isEmpty()) {
            return "Empty collection returned";
        }

        String itemType = collection.iterator().next().getClass().getSimpleName();
        return String.format("Collection of %d %s item%s", 
                           collection.size(), itemType, collection.size() == 1 ? "" : "s");
    }

    private static String generateMapSummary(Map<?, ?> map) {
        if (map.isEmpty()) {
            return "Empty map returned";
        }

        return String.format("Map with %d entr%s", 
                           map.size(), map.size() == 1 ? "y" : "ies");
    }

    private static String generatePOJOSummary(Object pojo, ToolResponse config) {
        Class<?> clazz = pojo.getClass();
        
        // Use custom template if provided
        if (config != null && !config.summaryTemplate().isEmpty()) {
            return formatCustomTemplate(pojo, config.summaryTemplate(), config);
        }

        StringBuilder summary = new StringBuilder();
        summary.append(clazz.getSimpleName());

        // Get field information
        Field[] fields = clazz.getDeclaredFields();
        if (fields.length > 0) {
            summary.append(" with ").append(fields.length).append(" propert")
                   .append(fields.length == 1 ? "y" : "ies");
        }

        // Add key field values
        String[] highlightFields = config != null ? config.highlightFields() : 
                                 new String[]{"name", "id", "title", "description"};
        
        String keyFieldInfo = extractKeyFieldInfo(pojo, highlightFields);
        if (!keyFieldInfo.isEmpty()) {
            summary.append(", ").append(keyFieldInfo);
        }

        return summary.toString();
    }

    private static String formatCustomTemplate(Object pojo, String template, ToolResponse config) {
        String result = template;
        Class<?> clazz = pojo.getClass();
        
        result = result.replace("{className}", clazz.getSimpleName());
        result = result.replace("{fieldCount}", String.valueOf(clazz.getDeclaredFields().length));
        
        String keyFields = extractKeyFieldInfo(pojo, config.highlightFields());
        result = result.replace("{keyFields}", keyFields);
        
        return result;
    }

    private static String extractKeyFieldInfo(Object pojo, String[] highlightFields) {
        StringBuilder keyInfo = new StringBuilder();
        Class<?> clazz = pojo.getClass();
        
        for (String fieldName : highlightFields) {
            try {
                Field field = findField(clazz, fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(pojo);
                    if (value != null && !value.toString().trim().isEmpty()) {
                        if (keyInfo.length() > 0) {
                            keyInfo.append(", ");
                        }
                        keyInfo.append(fieldName).append(": ").append(value);
                        break; // Only show the first matching field
                    }
                }
            } catch (Exception e) {
                // Ignore reflection errors
            }
        }
        
        return keyInfo.toString();
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        // Try exact match first
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // Try case-insensitive match
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(fieldName)) {
                    return field;
                }
            }
        }
        return null;
    }

    private static void addExecutionMetadata(ToolResult.ToolResultBuilder builder, Method method, Object result) {
        builder.metadata("executionTime", Instant.now().toEpochMilli());
        builder.metadata("methodName", method.getName());
        builder.metadata("methodClass", method.getDeclaringClass().getSimpleName());
        
        if (result != null) {
            builder.metadata("resultType", result.getClass().getSimpleName());
            
            if (result instanceof Collection) {
                builder.metadata("resultSize", ((Collection<?>) result).size());
            } else if (result instanceof Map) {
                builder.metadata("resultSize", ((Map<?, ?>) result).size());
            }
        }
    }
}
