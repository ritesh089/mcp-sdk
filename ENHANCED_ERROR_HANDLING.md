# Enhanced Error Handling and POJO Support

The MCP SDK now provides comprehensive error handling capabilities and POJO support, allowing developers to create more meaningful interactions with LLMs.

## 🎯 Key Features

### 1. Rich Error Messages for LLMs
- **Structured Error Information**: Errors include context, suggestions, and recovery guidance
- **LLM-Friendly Format**: Errors are formatted to help LLMs understand what went wrong and how to fix it
- **Multiple Error Types**: Validation, business logic, system, and permission errors

### 2. POJO Return Types
- **Natural Java Objects**: Return any Java object directly from tool methods
- **Automatic Serialization**: POJOs are automatically converted to JSON
- **Type Safety**: Compile-time checking for return types

### 3. Automatic Response Processing
- **Smart Content Generation**: Framework automatically generates appropriate content
- **Metadata Inclusion**: Execution metadata is automatically added
- **Flexible Configuration**: Customize responses with annotations

## 🚀 Quick Start

### Basic POJO Return
```java
@ToolMethod
public UserProfile getUserProfile(@Parameter String userId) throws MCPToolException {
    if (userId == null || userId.trim().isEmpty()) {
        throw MCPToolException.validation("User ID is required")
            .withSuggestedAction("Please provide a valid user ID")
            .withSuggestions("Try: 'user123'", "Try: 'john.doe'");
    }
    
    return userService.findById(userId); // Return POJO directly
}
```

### Rich Error Handling
```java
@ToolMethod
public CalculationResult calculate(@Parameter String expression) throws MCPToolException {
    if (expression == null || expression.trim().isEmpty()) {
        throw MCPToolException.validation("Expression cannot be empty")
            .withSuggestedAction("Please provide a mathematical expression like '2 + 3'")
            .withSuggestions("Try: '5 + 3'", "Try: '10 * 2.5'")
            .withContext("expectedFormat", "mathematical expression")
            .withContext("supportedOperations", Arrays.asList("+", "-", "*", "/"));
    }
    
    try {
        double result = evaluateExpression(expression);
        return new CalculationResult(result, expression, System.currentTimeMillis());
    } catch (ArithmeticException e) {
        throw MCPToolException.business("Mathematical operation failed")
            .withTechnicalDetails(e.getMessage())
            .withSuggestedAction("Check for division by zero")
            .withContext("expression", expression);
    }
}
```

## 📋 Error Types and Usage

### Validation Errors
For input validation failures:
```java
throw MCPToolException.validation("Parameter 'amount' must be positive")
    .withSuggestedAction("Please provide a positive number")
    .withContext("providedValue", amount)
    .withSuggestions("Try: 10.00", "Try: 25.50");
```

### Business Logic Errors
For business rule violations:
```java
throw MCPToolException.business("Insufficient funds for transaction")
    .withSuggestedAction("Add funds to your account or reduce the amount")
    .withContext("availableBalance", balance)
    .withContext("requestedAmount", amount)
    .withSuggestions("Reduce amount to " + balance, "Add funds to account");
```

### System Errors
For technical/system failures:
```java
throw MCPToolException.system("Database connection failed")
    .withTechnicalDetails(e.getMessage())
    .withSuggestedAction("Try again in a few moments")
    .withContext("errorCode", "DB_CONN_001");
```

### Permission Errors
For access control issues:
```java
throw MCPToolException.permission("Access denied to resource")
    .withSuggestedAction("Contact administrator for access")
    .withContext("requiredRole", "ADMIN")
    .withContext("userRole", currentUser.getRole());
```

## 🎨 Response Customization

### @ToolResponse Annotation
Customize how responses are processed:
```java
@ToolMethod
@ToolResponse(
    message = "File analysis completed successfully",
    description = "Returns detailed file analysis with statistics",
    summaryTemplate = "{className} for '{keyFields}' with {fieldCount} properties",
    highlightFields = {"filename", "size", "type"}
)
public FileAnalysisResult analyzeFile(@Parameter String filePath) throws MCPToolException {
    return performFileAnalysis(filePath);
}
```

### Builder Pattern for Complex Responses
For responses with multiple content types:
```java
@ToolMethod
public ToolResult generateReport(@Parameter String reportType) throws MCPToolException {
    ReportData data = generateReportData(reportType);
    String chartImage = generateChartImage(data);
    
    return ToolResult.builder()
        .message("Report generated successfully")
        .addText("Executive Summary: " + data.getSummary())
        .addData(data)
        .addImage(chartImage, "image/png")
        .metadata("reportId", UUID.randomUUID().toString())
        .build();
}
```

## 📊 Generated JSON Examples

### Rich Error Response
```json
{
  "content": [
    {
      "type": "error",
      "errorType": "validation",
      "userMessage": "Expression cannot be empty",
      "suggestedAction": "Please provide a mathematical expression like '2 + 3'",
      "suggestions": [
        "Try: '5 + 3'",
        "Try: '10 * 2.5'"
      ],
      "context": {
        "expectedFormat": "mathematical expression",
        "supportedOperations": ["+", "-", "*", "/"]
      },
      "errorCode": -32602
    }
  ]
}
```

### POJO Success Response
```json
{
  "content": [
    {
      "type": "text",
      "text": "CalculationResult with 5 properties, expression: 2 + 3"
    },
    {
      "type": "data",
      "dataType": "CalculationResult",
      "data": {
        "result": 5.0,
        "expression": "2 + 3",
        "operation": "addition",
        "timestamp": 1704067200000,
        "metadata": {}
      }
    }
  ],
  "metadata": {
    "executionTime": 1704067200000,
    "methodName": "calculate",
    "resultType": "CalculationResult"
  }
}
```

## 🔄 Migration from Legacy Code

### Before (Legacy)
```java
@ToolMethod
public ToolResult calculate(@Parameter String expression) {
    try {
        double result = evaluateExpression(expression);
        return ToolResult.success("Result: " + result);
    } catch (Exception e) {
        return ToolResult.error("Calculation failed: " + e.getMessage());
    }
}
```

### After (Enhanced)
```java
@ToolMethod
public CalculationResult calculate(@Parameter String expression) throws MCPToolException {
    if (expression == null || expression.trim().isEmpty()) {
        throw MCPToolException.validation("Expression cannot be empty")
            .withSuggestedAction("Please provide a mathematical expression")
            .withSuggestions("Try: '2 + 3'", "Try: '10 * 5'");
    }
    
    try {
        double result = evaluateExpression(expression);
        return new CalculationResult(result, expression, "calculation");
    } catch (ArithmeticException e) {
        throw MCPToolException.business("Mathematical operation failed")
            .withTechnicalDetails(e.getMessage())
            .withSuggestedAction("Check for division by zero")
            .withContext("expression", expression);
    }
}
```

## 🔧 Best Practices

### 1. Error Message Guidelines
- **User Message**: Clear, non-technical explanation for LLMs
- **Technical Details**: Detailed information for debugging
- **Suggested Action**: Specific guidance on how to fix the issue
- **Context**: Relevant data that helps understand the error
- **Suggestions**: Concrete examples of valid inputs

### 2. POJO Design
- Use meaningful class and field names
- Include key identifying fields (id, name, title)
- Provide proper getters and setters
- Consider including metadata fields

### 3. Response Configuration
- Use `@ToolResponse` for custom messages
- Highlight important fields in summaries
- Include relevant metadata
- Use builder pattern for complex responses

## 🎯 Benefits for LLM Communication

### Better Error Understanding
- **Context-Aware**: LLMs understand what went wrong and why
- **Actionable**: Clear guidance on how to fix issues
- **Educational**: Suggestions help LLMs learn better patterns

### Structured Data
- **Type Safety**: Consistent data structures
- **Rich Content**: Multiple content types in single response
- **Metadata**: Additional context for processing

### Developer Experience
- **Natural Code**: Write normal Java methods
- **Type Safety**: Compile-time checking
- **Less Boilerplate**: Framework handles serialization

This enhanced error handling system transforms how MCP tools communicate with LLMs, providing rich, actionable information that enables more intelligent and helpful interactions.
