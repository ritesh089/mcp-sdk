package com.mcp.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for customizing how MCP tool method responses are processed and formatted.
 * This annotation allows developers to configure response messages, content types,
 * and metadata inclusion for their tool methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ToolResponse {
    
    /**
     * Success message to include in the response.
     * If empty, a default message will be generated based on the method name and return type.
     * 
     * @return the success message
     */
    String message() default "";
    
    /**
     * Expected content types that this method may return.
     * This is used for documentation and validation purposes.
     * Common values: "text", "data", "image", "error"
     * 
     * @return array of expected content types
     */
    String[] contentTypes() default {};
    
    /**
     * Whether to include execution metadata in the response.
     * Metadata includes execution time, method name, result type, etc.
     * 
     * @return true to include metadata, false otherwise
     */
    boolean includeMetadata() default true;
    
    /**
     * Description of what this method returns.
     * Used for documentation and schema generation.
     * 
     * @return description of the response
     */
    String description() default "";
    
    /**
     * Whether to automatically generate a text summary of POJO responses.
     * When true, the framework will create a human-readable summary of complex objects.
     * 
     * @return true to generate summary, false otherwise
     */
    boolean generateSummary() default true;
    
    /**
     * Custom summary template for POJO responses.
     * Can use placeholders like {className}, {fieldCount}, {keyFields}.
     * If empty, a default template will be used.
     * 
     * @return custom summary template
     */
    String summaryTemplate() default "";
    
    /**
     * Fields to highlight in the summary for POJO responses.
     * These fields will be included in the text summary if they exist and have values.
     * 
     * @return array of field names to highlight
     */
    String[] highlightFields() default {"name", "id", "title", "description"};
}
