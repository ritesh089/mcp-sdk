package com.mcp.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as an MCP Resource.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MCPResource {
    
    /**
     * The URI pattern this resource handles (e.g., "file://", "config://system").
     */
    String uri();
    
    /**
     * Human-readable name for this resource.
     */
    String name() default "";
    
    /**
     * Description of what this resource provides.
     */
    String description();
    
    /**
     * The MIME type of the resource content.
     */
    String mimeType() default "application/json";
    
    /**
     * The event bus address for this resource. If not specified, uses auto-generated address.
     */
    String eventBusAddress() default "";
}