package com.mcp.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for tool method parameters to define validation and documentation.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    
    /**
     * Parameter name. If not specified, uses the parameter name from reflection.
     */
    String name() default "";
    
    /**
     * Description of the parameter.
     */
    String description();
    
    /**
     * Whether this parameter is required.
     */
    boolean required() default true;
    
    /**
     * Default value if parameter is not provided (only for optional parameters).
     */
    String defaultValue() default "";
    
    /**
     * Minimum value for numeric parameters.
     */
    double min() default Double.NEGATIVE_INFINITY;
    
    /**
     * Maximum value for numeric parameters.
     */
    double max() default Double.POSITIVE_INFINITY;
    
    /**
     * Valid enum values for string parameters.
     */
    String[] enumValues() default {};
    
    /**
     * Regular expression pattern for string validation.
     */
    String pattern() default "";
}