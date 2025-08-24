package com.mcp.sdk.examples;

import com.mcp.sdk.AnnotatedMCPTool;
import com.mcp.sdk.MCPToolException;
import com.mcp.sdk.ToolResult;
import com.mcp.sdk.annotations.*;
import java.util.*;

/**
 * Example demonstrating the enhanced MCP SDK capabilities:
 * - POJO return types
 * - Rich error handling with meaningful messages for LLMs
 * - Automatic response processing
 * - Custom response configuration
 */
@MCPTool(
    name = "enhanced_calculator",
    description = "Advanced calculator with rich error handling and structured responses"
)
public class EnhancedCalculatorExample extends AnnotatedMCPTool {

    // Example POJO for structured responses
    public static class CalculationResult {
        private double result;
        private String expression;
        private String operation;
        private long timestamp;
        private Map<String, Object> metadata;

        public CalculationResult(double result, String expression, String operation) {
            this.result = result;
            this.expression = expression;
            this.operation = operation;
            this.timestamp = System.currentTimeMillis();
            this.metadata = new HashMap<>();
        }

        // Getters and setters
        public double getResult() { return result; }
        public void setResult(double result) { this.result = result; }
        
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * Example 1: Simple string return with rich error handling
     */
    @ToolMethod
    @ToolResponse(
        message = "Basic calculation completed",
        description = "Performs basic arithmetic operations"
    )
    public String basicCalculation(
        @Parameter(name = "expression", description = "Mathematical expression to evaluate", required = true)
        String expression
    ) throws MCPToolException {
        
        if (expression == null || expression.trim().isEmpty()) {
            throw MCPToolException.validation("Expression cannot be empty")
                .withSuggestedAction("Please provide a mathematical expression like '2 + 3' or '10 * 5'")
                .withSuggestions(
                    "Try: '5 + 3'",
                    "Try: '10 - 4'", 
                    "Try: '6 * 7'",
                    "Try: '20 / 4'"
                )
                .withContext("expectedFormat", "mathematical expression")
                .withContext("supportedOperations", Arrays.asList("+", "-", "*", "/"));
        }

        try {
            double result = evaluateBasicExpression(expression);
            return String.format("The result of '%s' is %.2f", expression, result);
        } catch (ArithmeticException e) {
            throw MCPToolException.business("Mathematical operation failed")
                .withTechnicalDetails(e.getMessage())
                .withSuggestedAction("Check for division by zero or invalid operations")
                .withContext("expression", expression)
                .withSuggestions(
                    "Ensure denominators are not zero",
                    "Check for valid mathematical operations",
                    "Use parentheses for complex expressions"
                );
        }
    }

    /**
     * Example 2: POJO return with custom response configuration
     */
    @ToolMethod
    @ToolResponse(
        message = "Advanced calculation completed successfully",
        description = "Returns detailed calculation results with metadata",
        summaryTemplate = "{className} for '{keyFields}' with {fieldCount} properties"
    )
    public CalculationResult advancedCalculation(
        @Parameter(name = "expression", description = "Mathematical expression", required = true)
        String expression,
        @Parameter(name = "precision", description = "Number of decimal places", required = false, min = 0, max = 10)
        int precision
    ) throws MCPToolException {
        
        if (expression == null || expression.trim().isEmpty()) {
            throw MCPToolException.validation("Expression is required")
                .withSuggestedAction("Please provide a valid mathematical expression")
                .withContext("parameter", "expression")
                .withSuggestions("Example: '(5 + 3) * 2'", "Example: 'sqrt(16) + 4'");
        }

        if (containsUnsupportedFunction(expression)) {
            String unsupportedFunc = getUnsupportedFunction(expression);
            throw MCPToolException.business("Unsupported mathematical function detected")
                .withTechnicalDetails("Function '" + unsupportedFunc + "' is not available in this calculator")
                .withSuggestedAction("Use supported functions: +, -, *, /, sqrt, pow, sin, cos, tan")
                .withContext("unsupportedFunction", unsupportedFunc)
                .withContext("supportedFunctions", Arrays.asList("+", "-", "*", "/", "sqrt", "pow", "sin", "cos", "tan"))
                .withSuggestions(
                    "Replace '" + unsupportedFunc + "' with a supported function",
                    "Break down complex expressions into simpler parts",
                    "Use basic arithmetic operations"
                );
        }

        try {
            double result = evaluateAdvancedExpression(expression);
            
            // Round to specified precision
            if (precision > 0) {
                double factor = Math.pow(10, precision);
                result = Math.round(result * factor) / factor;
            }

            CalculationResult calcResult = new CalculationResult(result, expression, "advanced");
            calcResult.getMetadata().put("precision", precision);
            calcResult.getMetadata().put("complexity", analyzeComplexity(expression));
            
            return calcResult;
            
        } catch (ArithmeticException e) {
            throw MCPToolException.business("Calculation failed")
                .withTechnicalDetails(e.getMessage())
                .withSuggestedAction("Review the expression for mathematical errors")
                .withContext("expression", expression)
                .withContext("precision", precision);
        } catch (Exception e) {
            throw MCPToolException.system("Unexpected error during calculation")
                .withTechnicalDetails(e.getMessage())
                .withSuggestedAction("Try a simpler expression or contact support")
                .withContext("expression", expression);
        }
    }

    /**
     * Example 3: Collection return with error handling
     */
    @ToolMethod
    @ToolResponse(
        message = "Calculation history retrieved",
        description = "Returns a list of recent calculations"
    )
    public List<CalculationResult> getCalculationHistory(
        @Parameter(name = "limit", description = "Maximum number of results", required = false, min = 1, max = 100)
        int limit
    ) throws MCPToolException {
        
        if (limit <= 0) {
            throw MCPToolException.validation("Limit must be a positive number")
                .withSuggestedAction("Please provide a number between 1 and 100")
                .withContext("providedLimit", limit)
                .withContext("validRange", "1-100")
                .withSuggestions("Try: 10", "Try: 25", "Try: 50");
        }

        if (limit > 100) {
            throw MCPToolException.business("Limit exceeds maximum allowed value")
                .withSuggestedAction("Please use a smaller limit (maximum 100)")
                .withContext("requestedLimit", limit)
                .withContext("maximumLimit", 100)
                .withSuggestions("Try: 100", "Try: 50", "Use pagination for larger datasets");
        }

        // Simulate getting calculation history
        List<CalculationResult> history = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, 5); i++) {
            CalculationResult result = new CalculationResult(
                Math.random() * 100, 
                "sample_expression_" + i, 
                "historical"
            );
            history.add(result);
        }

        return history;
    }

    /**
     * Example 4: Using ToolResult builder for complex responses
     */
    @ToolMethod
    public ToolResult complexCalculation(
        @Parameter(name = "expressions", description = "Array of expressions to evaluate", required = true)
        String[] expressions
    ) throws MCPToolException {
        
        if (expressions == null || expressions.length == 0) {
            throw MCPToolException.validation("At least one expression is required")
                .withSuggestedAction("Please provide an array of mathematical expressions")
                .withSuggestions("Example: ['2+2', '3*4', '10/2']");
        }

        List<CalculationResult> results = new ArrayList<>();
        StringBuilder summary = new StringBuilder("Processed " + expressions.length + " expressions:\n");

        for (int i = 0; i < expressions.length; i++) {
            try {
                double result = evaluateBasicExpression(expressions[i]);
                CalculationResult calcResult = new CalculationResult(result, expressions[i], "batch");
                results.add(calcResult);
                summary.append(String.format("- %s = %.2f\n", expressions[i], result));
            } catch (Exception e) {
                throw MCPToolException.business("Failed to evaluate expression at index " + i)
                    .withTechnicalDetails(e.getMessage())
                    .withSuggestedAction("Check the expression syntax and try again")
                    .withContext("failedExpression", expressions[i])
                    .withContext("expressionIndex", i)
                    .withContext("totalExpressions", expressions.length);
            }
        }

        return ToolResult.builder()
            .message("Batch calculation completed successfully")
            .addText(summary.toString())
            .addData(results)
            .metadata("totalExpressions", expressions.length)
            .metadata("successfulCalculations", results.size())
            .build();
    }

    // Helper methods for demonstration
    private double evaluateBasicExpression(String expression) {
        // Simple evaluation for demo purposes
        expression = expression.trim();
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            return Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim());
        } else if (expression.contains("-")) {
            String[] parts = expression.split("-");
            return Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim());
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            return Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim());
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/");
            double denominator = Double.parseDouble(parts[1].trim());
            if (denominator == 0) {
                throw new ArithmeticException("Division by zero");
            }
            return Double.parseDouble(parts[0].trim()) / denominator;
        }
        return Double.parseDouble(expression);
    }

    private double evaluateAdvancedExpression(String expression) {
        // More complex evaluation would go here
        return evaluateBasicExpression(expression);
    }

    private boolean containsUnsupportedFunction(String expression) {
        return expression.contains("log") || expression.contains("exp") || expression.contains("factorial");
    }

    private String getUnsupportedFunction(String expression) {
        if (expression.contains("log")) return "log";
        if (expression.contains("exp")) return "exp";
        if (expression.contains("factorial")) return "factorial";
        return "unknown";
    }

    private String analyzeComplexity(String expression) {
        if (expression.contains("(") || expression.contains(")")) return "complex";
        if (expression.split("[+\\-*/]").length > 2) return "medium";
        return "simple";
    }
}
