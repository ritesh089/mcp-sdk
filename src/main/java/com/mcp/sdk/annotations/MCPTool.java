package com.mcp.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as an MCP Tool.
 * Eliminates the need for manual JSON configuration files.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MCPTool {
    
    /**
     * The name of the tool. If not specified, uses the class name in lowercase.
     */
    String name() default "";
    
    /**
     * Description of what this tool does.
     */
    String description();
    
    /**
     * The event bus address for this tool. If not specified, uses "tools.{name}".
     */
    String eventBusAddress() default "";
    
    /**
     * Whether this tool supports streaming responses.
     */
    boolean streaming() default false;
    
    /**
     * Tags for categorizing tools.
     */
    String[] tags() default {};
    
    /**
     * Examples of tool usage.
     */
    ToolExample[] examples() default {};
}