package com.mcp.sdk.testing;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Represents the result of a test operation with fluent assertion methods.
 */
public class TestResult {
    
    private final JsonObject data;
    private final boolean success;
    private final String errorMessage;

    private TestResult(JsonObject data, boolean success, String errorMessage) {
        this.data = data;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static TestResult success(JsonObject data) {
        return new TestResult(data, true, null);
    }

    public static TestResult error(String message) {
        return new TestResult(null, false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JsonObject getData() {
        return data;
    }

    /**
     * Assert that the operation was successful.
     */
    public TestResult assertSuccess() {
        if (!success) {
            throw new AssertionError("Expected success but got error: " + errorMessage);
        }
        return this;
    }

    /**
     * Assert that the operation failed.
     */
    public TestResult assertError() {
        if (success) {
            throw new AssertionError("Expected error but operation was successful");
        }
        return this;
    }

    /**
     * Assert that the error message contains the specified text.
     */
    public TestResult assertErrorContains(String expectedText) {
        assertError();
        if (errorMessage == null || !errorMessage.contains(expectedText)) {
            throw new AssertionError("Expected error message to contain '" + expectedText + 
                "' but was: " + errorMessage);
        }
        return this;
    }

    /**
     * Assert that the result contains the specified content.
     */
    public TestResult assertContains(String field, Object expectedValue) {
        assertSuccess();
        if (data == null) {
            throw new AssertionError("No data available for assertion");
        }
        
        Object actualValue = data.getValue(field);
        if (!expectedValue.equals(actualValue)) {
            throw new AssertionError("Expected field '" + field + "' to be '" + expectedValue + 
                "' but was: " + actualValue);
        }
        return this;
    }

    /**
     * Assert that the result has content (for tools).
     */
    public TestResult assertHasContent() {
        assertSuccess();
        if (data == null || !data.containsKey("content")) {
            throw new AssertionError("Expected result to have 'content' field");
        }
        return this;
    }

    /**
     * Assert that the content contains text.
     */
    public TestResult assertContentContains(String expectedText) {
        assertHasContent();
        JsonArray content = data.getJsonArray("content");
        if (content == null || content.isEmpty()) {
            throw new AssertionError("Content array is empty");
        }
        
        boolean found = false;
        for (int i = 0; i < content.size(); i++) {
            JsonObject contentItem = content.getJsonObject(i);
            if (contentItem != null && "text".equals(contentItem.getString("type"))) {
                String text = contentItem.getString("text");
                if (text != null && text.contains(expectedText)) {
                    found = true;
                    break;
                }
            }
        }
        
        if (!found) {
            throw new AssertionError("Expected content to contain '" + expectedText + 
                "' but it was not found in: " + content.encode());
        }
        return this;
    }

    /**
     * Assert that the result has contents (for resources).
     */
    public TestResult assertHasContents() {
        assertSuccess();
        if (data == null || !data.containsKey("contents")) {
            throw new AssertionError("Expected result to have 'contents' field");
        }
        return this;
    }

    /**
     * Assert that the result has messages (for prompts).
     */
    public TestResult assertHasMessages() {
        assertSuccess();
        if (data == null || !data.containsKey("messages")) {
            throw new AssertionError("Expected result to have 'messages' field");
        }
        return this;
    }

    /**
     * Assert that the messages contain the expected text.
     */
    public TestResult assertMessagesContain(String expectedText) {
        assertHasMessages();
        JsonArray messages = data.getJsonArray("messages");
        if (messages == null || messages.isEmpty()) {
            throw new AssertionError("Messages array is empty");
        }
        
        boolean found = false;
        for (int i = 0; i < messages.size(); i++) {
            JsonObject message = messages.getJsonObject(i);
            if (message != null) {
                JsonObject content = message.getJsonObject("content");
                if (content != null && "text".equals(content.getString("type"))) {
                    String text = content.getString("text");
                    if (text != null && text.contains(expectedText)) {
                        found = true;
                        break;
                    }
                }
            }
        }
        
        if (!found) {
            throw new AssertionError("Expected messages to contain '" + expectedText + 
                "' but it was not found in: " + messages.encode());
        }
        return this;
    }

    /**
     * Custom assertion with predicate.
     */
    public TestResult assertThat(java.util.function.Predicate<JsonObject> predicate, String errorMessage) {
        assertSuccess();
        if (data == null || !predicate.test(data)) {
            throw new AssertionError(errorMessage + ". Actual data: " + (data != null ? data.encode() : "null"));
        }
        return this;
    }

    /**
     * Get a nested value from the result data.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue(String path) {
        if (data == null) return null;
        
        String[] parts = path.split("\\.");
        Object current = data.getMap();
        
        for (String part : parts) {
            if (current instanceof JsonObject) {
                current = ((JsonObject) current).getValue(part);
            } else if (current instanceof java.util.Map) {
                current = ((java.util.Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        
        return (T) current;
    }

    @Override
    public String toString() {
        if (success) {
            return "TestResult{success=true, data=" + (data != null ? data.encode() : "null") + "}";
        } else {
            return "TestResult{success=false, error='" + errorMessage + "'}";
        }
    }
}