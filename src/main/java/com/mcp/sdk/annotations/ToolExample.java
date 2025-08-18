package com.mcp.sdk.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines an example usage of a tool.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolExample {
    
    /**
     * Description of what this example demonstrates.
     */
    String description();
    
    /**
     * JSON string representing the input parameters.
     */
    String input();
    
    /**
     * Optional description of the expected output.
     */
    String expectedOutput() default "";
}