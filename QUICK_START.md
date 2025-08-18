# MCP SDK Quick Start Guide

Get up and running with the MCP SDK in minutes!

## Add to Your Project

### Gradle

```gradle
dependencies {
    implementation 'io.github.ritesh089:mcp-sdk:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.ritesh089</groupId>
    <artifactId>mcp-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Create Your First MCP Tool

```java
import com.mcp.sdk.annotations.MCPTool;
import com.mcp.sdk.annotations.ToolMethod;
import com.mcp.sdk.annotations.Parameter;
import com.mcp.sdk.ToolResult;

@MCPTool(
    name = "greeter",
    description = "A simple greeting tool"
)
public class GreeterTool {
    
    @ToolMethod(
        name = "greet",
        description = "Greets a person by name"
    )
    public ToolResult greet(
        @Parameter(name = "name", description = "Name to greet") String name,
        @Parameter(name = "language", description = "Language for greeting", defaultValue = "en") String language
    ) {
        String greeting;
        switch (language.toLowerCase()) {
            case "es":
                greeting = "¡Hola, " + name + "!";
                break;
            case "fr":
                greeting = "Bonjour, " + name + "!";
                break;
            default:
                greeting = "Hello, " + name + "!";
        }
        
        return ToolResult.success(greeting);
    }
}
```

## Create an MCP Resource

```java
import com.mcp.sdk.annotations.MCPResource;
import com.mcp.sdk.MCPResource;
import io.vertx.core.json.JsonObject;

@MCPResource(
    uri = "system/info",
    name = "System Information",
    description = "Provides system information"
)
public class SystemInfoResource extends MCPResource {
    
    @Override
    protected void start() {
        // Register the resource
        super.start();
    }
    
    @Override
    protected JsonObject getContent() {
        return new JsonObject()
            .put("os", System.getProperty("os.name"))
            .put("version", System.getProperty("os.version"))
            .put("arch", System.getProperty("os.arch"))
            .put("java", System.getProperty("java.version"));
    }
}
```

## Create an MCP Prompt

```java
import com.mcp.sdk.annotations.MCPPrompt;
import com.mcp.sdk.MCPPrompt;
import io.vertx.core.json.JsonObject;

@MCPPrompt(
    name = "code_review",
    description = "Performs automated code review",
    arguments = {"file_path", "language"}
)
public class CodeReviewPrompt extends MCPPrompt {
    
    @Override
    protected void start() {
        // Register the prompt
        super.start();
    }
    
    @Override
    protected JsonObject execute(JsonObject arguments) {
        String filePath = arguments.getString("file_path");
        String language = arguments.getString("language");
        
        // Your code review logic here
        JsonObject result = new JsonObject()
            .put("file", filePath)
            .put("language", language)
            .put("issues", new JsonArray())
            .put("score", 95);
            
        return result;
    }
}
```

## Testing Your Tools

```java
import com.mcp.sdk.testing.MCPTestServer;
import com.mcp.sdk.testing.TestResult;
import org.junit.Test;
import static org.junit.Assert.*;

public class GreeterToolTest {
    
    @Test
    public void testGreeting() {
        MCPTestServer server = MCPTestServer.create()
            .withTool(new GreeterTool())
            .start();
        
        TestResult result = server.callTool("greet", 
            new JsonObject()
                .put("name", "World")
                .put("language", "en"));
        
        assertTrue(result.isSuccess());
        assertTrue(result.getContent().contains("Hello, World!"));
        
        server.stop();
    }
}
```

## Advanced Features

### Streaming Tools

```java
@MCPTool(
    name = "streaming_calculator",
    description = "A streaming calculator",
    streaming = true
)
public class StreamingCalculatorTool extends MCPStreamingTool {
    
    @ToolMethod(name = "calculate", description = "Streams calculation steps")
    public void calculate(double a, double b, String operation) {
        // Stream intermediate results
        streamChunk(new JsonObject()
            .put("step", "Starting calculation")
            .put("a", a)
            .put("b", b)
            .put("operation", operation));
        
        double result = 0;
        switch (operation) {
            case "add":
                result = a + b;
                break;
            case "multiply":
                result = a * b;
                break;
        }
        
        // Stream final result
        streamResult(new JsonObject()
            .put("result", result)
            .put("operation", operation));
    }
}
```

### Context Support

```java
@ToolMethod(name = "contextual_greeting")
public ToolResult contextualGreeting(
    @Parameter(name = "name") String name,
    MCPContext context
) {
    // Access correlation ID for tracing
    String correlationId = context.getCorrelationId().orElse("unknown");
    
    // Add custom attributes
    context.setAttribute("last_greeting", System.currentTimeMillis());
    
    return ToolResult.success("Hello, " + name + "!")
        .addData("correlation_id", correlationId)
        .addData("timestamp", System.currentTimeMillis());
}
```

## Configuration

The SDK automatically discovers your tools, resources, and prompts using annotations. You can also manually configure them:

```java
import com.mcp.sdk.config.AnnotationConfigProcessor;
import com.mcp.sdk.config.MCPConfiguration;

AnnotationConfigProcessor processor = new AnnotationConfigProcessor();
MCPConfiguration config = processor.autoDiscover("com.example.mcp");

// Or manually add components
config.addTool(new MyTool());
config.addResource(new MyResource());
config.addPrompt(new MyPrompt());
```

## What's Next?

- Check out the [full documentation](README.md)
- Explore the [Maven Central setup guide](MAVEN_CENTRAL_SETUP.md)
- Look at the [examples directory](../mcp-java-examples/) for more complex use cases
- Join the community discussions on GitHub

## Need Help?

- Create an issue on GitHub
- Check the documentation
- Review existing issues and discussions
