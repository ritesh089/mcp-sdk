# MCP SDK

A lightweight SDK for building MCP (Model Context Protocol) tools with context support and annotations.

## Features

- **MCP Tool Support**: Easy-to-use annotations for creating MCP tools
- **Context Management**: Built-in context support for maintaining state across tool calls
- **Resource Management**: Support for MCP resources with annotations
- **Prompt Management**: MCP prompt support with annotation-based configuration
- **HTTP Streaming**: Support for HTTP streaming tools
- **Event Logging**: Built-in event logging and correlation tracking

## Quick Start

### Adding to your project

```gradle
dependencies {
    implementation 'io.github.ritesh089:mcp-tool-sdk:1.0.0'
}
```

### Creating a simple MCP tool

```java
import com.mcp.sdk.annotations.MCPTool;
import com.mcp.sdk.annotations.ToolMethod;
import com.mcp.sdk.annotations.Parameter;

@MCPTool(
    name = "calculator",
    description = "A simple calculator tool"
)
public class CalculatorTool {
    
    @ToolMethod(
        name = "add",
        description = "Adds two numbers"
    )
    public ToolResult add(
        @Parameter(name = "a", description = "First number") double a,
        @Parameter(name = "b", description = "Second number") double b
    ) {
        return ToolResult.success(a + b);
    }
}
```

## Publishing

This project is configured to publish to multiple repositories:

### Maven Central (OSSRH)

To publish to Maven Central, you need:

1. **Sonatype OSSRH Account**: Sign up at [OSSRH](https://issues.sonatype.org/)
2. **GPG Key**: Create and upload a GPG key for signing
3. **Credentials**: Configure in `gradle.properties`

```bash
# Publish to Maven Central
./gradlew publishToMavenCentral
```

### GitHub Packages

To publish to GitHub Packages:

```bash
# Publish to GitHub Packages
./gradlew publishGitHub
```

### Local Testing

For local testing:

```bash
# Publish to local repository
./gradlew publishLocal
```

## Configuration

### gradle.properties

Create a `gradle.properties` file with your credentials:

```properties
# Maven Central / Sonatype OSSRH
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password

# Signing
signing.keyId=your-gpg-key-id
signing.key=your-gpg-private-key
signing.password=your-gpg-password

# GitHub Packages
gpr.user=ritesh089
gpr.key=your-github-personal-access-token
```

### GPG Setup

1. Generate a GPG key:
   ```bash
   gpg --gen-key
   ```

2. Export the private key:
   ```bash
   gpg --export-secret-keys --armor your-email@example.com > private-key.asc
   ```

3. Upload the public key to a key server:
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys your-key-id
   ```

## Development

### Building

```bash
./gradlew build
```

### Testing

```bash
./gradlew test
```

### Running Tests with Coverage

```bash
./gradlew test jacocoTestReport
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## Support

For issues and questions:
- Create an issue on GitHub
- Check the documentation
- Review existing issues and discussions
