package com.mcp.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as an MCP Prompt.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MCPPrompt {
    
    /**
     * The name of the prompt. If not specified, uses the class name in lowercase.
     */
    String name() default "";
    
    /**
     * Description of what this prompt generates.
     */
    String description();
    
    /**
     * The event bus address for this prompt. If not specified, uses "prompt.{name}".
     */
    String eventBusAddress() default "";
    
    /**
     * Tags for categorizing prompts.
     */
    String[] tags() default {};
    
    /**
     * Arguments for the prompt in format "name:type:description".
     */
    String[] arguments() default {};
}