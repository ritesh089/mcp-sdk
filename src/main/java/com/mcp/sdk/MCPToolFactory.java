package com.mcp.sdk;

/**
 * Factory class for creating MCP tools that supports both inheritance and interface patterns.
 * 
 * This factory automatically detects the type of tool implementation and creates
 * the appropriate wrapper. It provides a unified way to deploy tools regardless
 * of whether they extend AnnotatedMCPTool or implement MCPToolHandler.
 * 
 * Example usage:
 * <pre>
 * {@code
 * // For interface-based tools
 * MCPToolHandler calculator = new CalculatorTool();
 * AnnotatedMCPTool tool = MCPToolFactory.createTool(calculator);
 * 
 * // For inheritance-based tools (backward compatibility)
 * AnnotatedMCPTool legacy = new LegacyTool();
 * AnnotatedMCPTool tool = MCPToolFactory.createTool(legacy);
 * 
 * // Universal method (auto-detects type)
 * Object anyTool = new CalculatorTool(); // or new LegacyTool()
 * AnnotatedMCPTool tool = MCPToolFactory.createTool(anyTool);
 * }
 * </pre>
 */
public class MCPToolFactory {
    
    /**
     * Create an MCP tool from a tool handler implementation.
     * 
     * @param handler the tool handler implementation
     * @return an AnnotatedMCPTool ready for deployment
     * @throws IllegalArgumentException if handler is null
     */
    public static AnnotatedMCPTool createTool(MCPToolHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("MCPToolHandler cannot be null");
        }
        return new AnnotatedMCPToolAdapter(handler);
    }
    
    /**
     * Create an MCP tool from an existing AnnotatedMCPTool (pass-through for backward compatibility).
     * 
     * @param tool the existing tool implementation
     * @return the same tool instance
     * @throws IllegalArgumentException if tool is null
     */
    public static AnnotatedMCPTool createTool(AnnotatedMCPTool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("AnnotatedMCPTool cannot be null");
        }
        return tool;
    }
    
    /**
     * Universal tool creation method that auto-detects the tool type.
     * 
     * This method automatically determines whether the input is a tool handler
     * or an existing tool implementation and creates the appropriate wrapper.
     * 
     * @param toolImplementation the tool implementation (MCPToolHandler or AnnotatedMCPTool)
     * @return an AnnotatedMCPTool ready for deployment
     * @throws IllegalArgumentException if toolImplementation is null or unsupported type
     */
    public static AnnotatedMCPTool createTool(Object toolImplementation) {
        if (toolImplementation == null) {
            throw new IllegalArgumentException("Tool implementation cannot be null");
        }
        
        if (toolImplementation instanceof MCPToolHandler) {
            return createTool((MCPToolHandler) toolImplementation);
        } else if (toolImplementation instanceof AnnotatedMCPTool) {
            return createTool((AnnotatedMCPTool) toolImplementation);
        } else {
            throw new IllegalArgumentException(
                "Tool implementation must be either MCPToolHandler or AnnotatedMCPTool, got: " + 
                toolImplementation.getClass().getName());
        }
    }
    
    /**
     * Check if an object is a valid MCP tool implementation.
     * 
     * @param obj the object to check
     * @return true if the object can be used to create an MCP tool
     */
    public static boolean isValidToolImplementation(Object obj) {
        return obj instanceof MCPToolHandler || obj instanceof AnnotatedMCPTool;
    }
    
    /**
     * Get the tool type for a given implementation.
     * 
     * @param toolImplementation the tool implementation
     * @return a string describing the tool type
     */
    public static String getToolType(Object toolImplementation) {
        if (toolImplementation instanceof MCPToolHandler) {
            return "Interface-based tool (MCPToolHandler)";
        } else if (toolImplementation instanceof AnnotatedMCPTool) {
            return "Inheritance-based tool (AnnotatedMCPTool)";
        } else {
            return "Unknown tool type";
        }
    }
}
