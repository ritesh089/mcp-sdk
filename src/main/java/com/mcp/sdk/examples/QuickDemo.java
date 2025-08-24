package com.mcp.sdk.examples;

import com.mcp.sdk.*;
import com.mcp.sdk.annotations.*;
import io.vertx.core.json.JsonObject;

/**
 * Quick demonstration of the enhanced MCP SDK capabilities.
 * Shows before/after comparison of error handling and POJO support.
 */
public class QuickDemo {
    
    // Example POJO
    public static class Person {
        private String name;
        private int age;
        private String email;
        
        public Person(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
        
        // Getters
        public String getName() { return name; }
        public int getAge() { return age; }
        public String getEmail() { return email; }
    }
    
    @com.mcp.sdk.annotations.MCPTool(name = "demo", description = "Demonstration of enhanced MCP SDK")
    public static class DemoTool extends AnnotatedMCPTool {
        
        // ========== NEW WAY: POJO Return with Rich Errors ==========
        
        @ToolMethod
        @ToolResponse(message = "Person created successfully")
        public Person createPersonNew(
            @Parameter(name = "name", description = "Person's name", required = true) String name,
            @Parameter(name = "age", description = "Person's age", required = true, min = 0, max = 150) int age,
            @Parameter(name = "email", description = "Person's email", required = false) String email
        ) throws MCPToolException {
            
            // Rich validation with helpful suggestions
            if (name == null || name.trim().isEmpty()) {
                throw MCPToolException.validation("Name is required")
                    .withSuggestedAction("Please provide a valid name")
                    .withSuggestions("Try: 'John Doe'", "Try: 'Alice Smith'")
                    .withContext("parameter", "name");
            }
            
            if (age < 0 || age > 150) {
                throw MCPToolException.validation("Age must be between 0 and 150")
                    .withSuggestedAction("Please provide a realistic age")
                    .withContext("providedAge", age)
                    .withContext("validRange", "0-150")
                    .withSuggestions("Try: 25", "Try: 30", "Try: 45");
            }
            
            if (email != null && !email.contains("@")) {
                throw MCPToolException.validation("Invalid email format")
                    .withSuggestedAction("Please provide a valid email address")
                    .withContext("providedEmail", email)
                    .withSuggestions("Try: 'user@example.com'", "Try: 'name@domain.org'");
            }
            
            // Return POJO directly - framework handles everything!
            return new Person(name.trim(), age, email);
        }
        
        // ========== OLD WAY: Manual ToolResult Construction ==========
        
        @ToolMethod
        public ToolResult createPersonOld(
            @Parameter(name = "name", description = "Person's name", required = true) String name,
            @Parameter(name = "age", description = "Person's age", required = true) int age,
            @Parameter(name = "email", description = "Person's email", required = false) String email
        ) {
            
            // Basic validation with generic errors
            if (name == null || name.trim().isEmpty()) {
                return ToolResult.error("Name is required");
            }
            
            if (age < 0 || age > 150) {
                return ToolResult.error("Invalid age: " + age);
            }
            
            if (email != null && !email.contains("@")) {
                return ToolResult.error("Invalid email: " + email);
            }
            
            // Manual JSON construction
            JsonObject personJson = new JsonObject()
                .put("name", name.trim())
                .put("age", age)
                .put("email", email);
                
            return ToolResult.success("Person created successfully", personJson);
        }
    }
    
    // Demo method to show the difference in generated JSON
    public static void main(String[] args) {
        System.out.println("=== Enhanced MCP SDK Demo ===");
        System.out.println();
        System.out.println("NEW WAY Benefits:");
        System.out.println("✅ Return POJOs directly");
        System.out.println("✅ Rich error messages with context and suggestions");
        System.out.println("✅ Automatic JSON serialization");
        System.out.println("✅ Type safety and less boilerplate");
        System.out.println("✅ Better LLM communication");
        System.out.println();
        System.out.println("OLD WAY Limitations:");
        System.out.println("❌ Manual ToolResult construction");
        System.out.println("❌ Generic error messages");
        System.out.println("❌ Manual JSON building");
        System.out.println("❌ More boilerplate code");
        System.out.println("❌ Less helpful for LLMs");
        System.out.println();
        System.out.println("The enhanced SDK provides a much better developer experience");
        System.out.println("while creating more meaningful interactions with LLMs!");
    }
}
