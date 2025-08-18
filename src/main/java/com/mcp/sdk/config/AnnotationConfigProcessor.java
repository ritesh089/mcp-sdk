package com.mcp.sdk.config;

import com.mcp.sdk.annotations.MCPPrompt;
import com.mcp.sdk.annotations.MCPResource;
import com.mcp.sdk.annotations.MCPTool;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Processes annotations to automatically generate MCP configuration files.
 * Supports convention over configuration by auto-discovering components.
 */
public class AnnotationConfigProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(AnnotationConfigProcessor.class);

    private final ClassLoader classLoader;


    public AnnotationConfigProcessor(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Scan packages for MCP components and generate configuration.
     */
    public MCPConfiguration processAnnotations(String... packages) {
        MCPConfiguration config = new MCPConfiguration();
        
        for (String packageName : packages) {
            List<Class<?>> classes = scanPackage(packageName);
            
            for (Class<?> clazz : classes) {
                processClass(clazz, config);
            }
        }
        
        return config;
    }

    /**
     * Auto-discover components based on package structure conventions.
     */
    public MCPConfiguration autoDiscover(String basePackage) {
        MCPConfiguration config = new MCPConfiguration();
        
        // Convention: tools in .tools package
        String toolsPackage = basePackage + ".tools";
        discoverInPackage(toolsPackage, MCPTool.class, config);
        
        // Convention: resources in .resources package
        String resourcesPackage = basePackage + ".resources";
        discoverInPackage(resourcesPackage, MCPResource.class, config);
        
        // Convention: prompts in .prompts package
        String promptsPackage = basePackage + ".prompts";
        discoverInPackage(promptsPackage, MCPPrompt.class, config);
        
        return config;
    }

    private void discoverInPackage(String packageName, Class<? extends Annotation> annotationType, MCPConfiguration config) {
        List<Class<?>> classes = scanPackage(packageName);
        
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(annotationType) || 
                shouldAutoAnnotate(clazz, annotationType)) {
                processClass(clazz, config);
            }
        }
    }

    private boolean shouldAutoAnnotate(Class<?> clazz, Class<? extends Annotation> annotationType) {
        // Auto-detect based on class name patterns and inheritance
        if (annotationType == MCPTool.class) {
            return clazz.getSimpleName().endsWith("Tool") && 
                   isSubclassOf(clazz, "MCPTool");
        } else if (annotationType == MCPResource.class) {
            return clazz.getSimpleName().endsWith("Resource") && 
                   isSubclassOf(clazz, "MCPResource");
        } else if (annotationType == MCPPrompt.class) {
            return clazz.getSimpleName().endsWith("Prompt") && 
                   isSubclassOf(clazz, "MCPPrompt");
        }
        return false;
    }

    private boolean isSubclassOf(Class<?> clazz, String baseClassName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            if (current.getSimpleName().equals(baseClassName)) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private void processClass(Class<?> clazz, MCPConfiguration config) {
        if (clazz.isAnnotationPresent(MCPTool.class)) {
            processToolClass(clazz, config);
        } else if (clazz.isAnnotationPresent(MCPResource.class)) {
            processResourceClass(clazz, config);
        } else if (clazz.isAnnotationPresent(MCPPrompt.class)) {
            processPromptClass(clazz, config);
        }
    }

    private void processToolClass(Class<?> clazz, MCPConfiguration config) {
        MCPTool annotation = clazz.getAnnotation(MCPTool.class);
        String name = annotation.name().isEmpty() ? 
            clazz.getSimpleName().toLowerCase().replace("tool", "") : annotation.name();
        
        String eventBusAddress = annotation.eventBusAddress().isEmpty() ? 
            "tools." + name : annotation.eventBusAddress();

        // Generate inputSchema from method parameters
        JsonObject inputSchema = generateInputSchema(clazz);

        // Create clean MCP definition (no internal fields)
        JsonObject mcpDefinition = new JsonObject()
            .put("name", name)
            .put("description", annotation.description())
            .put("streaming", annotation.streaming())
            .put("tags", new JsonArray(Arrays.asList(annotation.tags())))
            .put("inputSchema", inputSchema);

        ToolConfiguration toolConfig = new ToolConfiguration(
            name, clazz.getName(), eventBusAddress, mcpDefinition
        );
        
        config.addTool(toolConfig);
    }

    private void processResourceClass(Class<?> clazz, MCPConfiguration config) {
        MCPResource annotation = clazz.getAnnotation(MCPResource.class);
        String name = annotation.name().isEmpty() ? 
            clazz.getSimpleName().replace("Resource", "") : annotation.name();
        
        String eventBusAddress = annotation.eventBusAddress().isEmpty() ? 
            "resources." + annotation.uri().replaceAll("[^a-zA-Z0-9]", "_") : annotation.eventBusAddress();

        JsonObject definition = new JsonObject()
            .put("uri", annotation.uri())
            .put("name", name)
            .put("description", annotation.description())
            .put("mimeType", annotation.mimeType());

        ResourceConfiguration resourceConfig = new ResourceConfiguration(
            annotation.uri(), clazz.getName(), eventBusAddress, definition
        );
        
        config.addResource(resourceConfig);
    }

    private void processPromptClass(Class<?> clazz, MCPConfiguration config) {
        MCPPrompt annotation = clazz.getAnnotation(MCPPrompt.class);
        String name = annotation.name().isEmpty() ? 
            clazz.getSimpleName().toLowerCase().replace("prompt", "") : annotation.name();
        
        String eventBusAddress = annotation.eventBusAddress().isEmpty() ? 
            "prompt." + name : annotation.eventBusAddress();

        JsonObject definition = new JsonObject()
            .put("name", name)
            .put("description", annotation.description())
            .put("tags", new JsonArray(Arrays.asList(annotation.tags())));

        PromptConfiguration promptConfig = new PromptConfiguration(
            name, clazz.getName(), eventBusAddress, definition
        );
        
        config.addPrompt(promptConfig);
    }

    private List<Class<?>> scanPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String packagePath = packageName.replace('.', '/');
        
        try {
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                
                if (resource.getProtocol().equals("file")) {
                    // Scan file system directory
                    scanDirectory(new File(resource.getFile()), packageName, classes);
                } else if (resource.getProtocol().equals("jar")) {
                    // Scan JAR file
                    scanJar(resource, packagePath, classes);
                }
            }
        } catch (IOException e) {
            System.err.println("Error scanning package " + packageName + ": " + e.getMessage());
        }
        
        return classes;
    }

    private void scanDirectory(File directory, String packageName, List<Class<?>> classes) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = classLoader.loadClass(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    // Skip classes that can't be loaded
                }
            }
        }
    }

    private void scanJar(URL jarUrl, String packagePath, List<Class<?>> classes) {
        String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));
        
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    String className = entryName.replace('/', '.').replace(".class", "");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        // Skip classes that can't be loaded
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error scanning JAR " + jarPath + ": " + e.getMessage());
        }
    }

    /**
     * Generate MCP-compliant inputSchema from tool class method parameters.
     */
    private JsonObject generateInputSchema(Class<?> clazz) {
        JsonObject schema = new JsonObject()
            .put("type", "object")
            .put("properties", new JsonObject())
            .put("required", new JsonArray());

        JsonObject properties = schema.getJsonObject("properties");
        JsonArray required = schema.getJsonArray("required");

        // Find the tool method (annotated with @ToolMethod or has @Parameter annotations)
        java.lang.reflect.Method toolMethod = findToolMethod(clazz);
        if (toolMethod != null) {
            // Process method parameters
            java.lang.reflect.Parameter[] parameters = toolMethod.getParameters();
            for (java.lang.reflect.Parameter param : parameters) {
                com.mcp.sdk.annotations.Parameter paramAnnotation = 
                    param.getAnnotation(com.mcp.sdk.annotations.Parameter.class);
                
                if (paramAnnotation != null) {
                    String paramName = paramAnnotation.name().isEmpty() ? param.getName() : paramAnnotation.name();
                    
                    JsonObject paramSchema = new JsonObject()
                        .put("description", paramAnnotation.description());
                    
                    // Add type information based on Java type
                    addTypeToSchema(paramSchema, param.getType());
                    
                    // Add validation constraints
                    if (!paramAnnotation.pattern().isEmpty()) {
                        paramSchema.put("pattern", paramAnnotation.pattern());
                    }
                    if (paramAnnotation.min() != Double.NEGATIVE_INFINITY) {
                        paramSchema.put("minimum", paramAnnotation.min());
                    }
                    if (paramAnnotation.max() != Double.POSITIVE_INFINITY) {
                        paramSchema.put("maximum", paramAnnotation.max());
                    }
                    if (paramAnnotation.enumValues().length > 0) {
                        paramSchema.put("enum", new JsonArray(Arrays.asList(paramAnnotation.enumValues())));
                    }
                    
                    properties.put(paramName, paramSchema);
                    
                    if (paramAnnotation.required()) {
                        required.add(paramName);
                    }
                }
            }
        }

        return schema;
    }

    /**
     * Find the main tool method in a class.
     */
    private java.lang.reflect.Method findToolMethod(Class<?> clazz) {
        // Look for method annotated with @ToolMethod
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(com.mcp.sdk.annotations.ToolMethod.class)) {
                return method;
            }
        }
        
        // Fallback: look for method with @Parameter annotations
        for (java.lang.reflect.Method method : clazz.getDeclaredMethods()) {
            if (method.getParameterCount() > 0) {
                boolean hasParameterAnnotations = java.util.Arrays.stream(method.getParameters())
                    .anyMatch(p -> p.isAnnotationPresent(com.mcp.sdk.annotations.Parameter.class));
                if (hasParameterAnnotations) {
                    return method;
                }
            }
        }
        
        return null;
    }

    /**
     * Map Java types to JSON Schema types.
     */
    private void addTypeToSchema(JsonObject schema, Class<?> javaType) {
        if (javaType == String.class) {
            schema.put("type", "string");
        } else if (javaType == int.class || javaType == Integer.class) {
            schema.put("type", "integer");
        } else if (javaType == long.class || javaType == Long.class) {
            schema.put("type", "integer");
        } else if (javaType == double.class || javaType == Double.class) {
            schema.put("type", "number");
        } else if (javaType == float.class || javaType == Float.class) {
            schema.put("type", "number");
        } else if (javaType == boolean.class || javaType == Boolean.class) {
            schema.put("type", "boolean");
        } else if (javaType.isArray()) {
            schema.put("type", "array");
        } else {
            // Default to string for unknown types
            schema.put("type", "string");
        }
    }

    /**
     * Generate JSON schema files for MCP components during compilation.
     * Creates schema files in the schema/ subdirectory under the target directory.
     * 
     * @param outputDir the base output directory (e.g., src/main/resources)
     * @param packagePrefix the package prefix to scan (e.g., "com.example.deployable")
     */
    public void generateSchemaFiles(String outputDir, String packagePrefix) {
        try {
            // Create schema directory
            Path schemaDir = Paths.get(outputDir, "schema");
            Files.createDirectories(schemaDir);
            
            // Process annotations from the specified package
            MCPConfiguration config = processAnnotations(packagePrefix);
            
            // Generate tool schemas
            generateToolSchemas(config, schemaDir);
            
            // Generate resource schemas  
            generateResourceSchemas(config, schemaDir);
            
            // Generate prompt schemas
            generatePromptSchemas(config, schemaDir);
            
            // Generate combined MCP schema
            generateMcpSchema(config, schemaDir);
            
            // Schema generation complete - output handled by calling code
            
        } catch (IOException e) {
            logger.error("Failed to generate schema files", e);
            throw new RuntimeException("Schema generation failed", e);
        }
    }

    private void generateToolSchemas(MCPConfiguration config, Path schemaDir) throws IOException {
        Path toolsDir = schemaDir.resolve("tools");
        Files.createDirectories(toolsDir);
        
        JsonObject toolsConfig = config.toJson().getJsonObject("tools", new JsonObject());
        
        for (String toolName : toolsConfig.fieldNames()) {
            JsonObject toolDef = toolsConfig.getJsonObject(toolName);
            // Get the definition from the nested structure
            JsonObject definition = toolDef.getJsonObject("definition", toolDef);
            
            JsonObject toolSchema = new JsonObject()
                .put("$schema", "http://json-schema.org/draft-07/schema#")
                .put("type", "object")
                .put("title", "MCP Tool: " + toolName)
                .put("description", definition.getString("description", ""))
                .put("properties", new JsonObject()
                    .put("name", new JsonObject()
                        .put("type", "string")
                        .put("const", toolName))
                    .put("arguments", definition.getJsonObject("inputSchema", new JsonObject())))
                .put("required", new JsonArray().add("name").add("arguments"));
            
            writeJsonFile(toolsDir.resolve(toolName + ".json"), toolSchema);
        }
    }

    private void generateResourceSchemas(MCPConfiguration config, Path schemaDir) throws IOException {
        Path resourcesDir = schemaDir.resolve("resources");
        Files.createDirectories(resourcesDir);
        
        JsonObject resourcesConfig = config.toJson().getJsonObject("resources", new JsonObject());
        
        for (String resourceUri : resourcesConfig.fieldNames()) {
            JsonObject resourceDef = resourcesConfig.getJsonObject(resourceUri);
            String resourceName = resourceUri.replaceAll("[^a-zA-Z0-9_-]", "_");
            
            JsonObject resourceSchema = new JsonObject()
                .put("$schema", "http://json-schema.org/draft-07/schema#")
                .put("type", "object")
                .put("title", "MCP Resource: " + resourceUri)
                .put("description", resourceDef.getString("description", ""))
                .put("properties", new JsonObject()
                    .put("uri", new JsonObject()
                        .put("type", "string")
                        .put("const", resourceUri))
                    .put("mimeType", new JsonObject()
                        .put("type", "string")
                        .put("default", resourceDef.getString("mimeType", "application/json"))))
                .put("required", new JsonArray().add("uri"));
            
            writeJsonFile(resourcesDir.resolve(resourceName + ".json"), resourceSchema);
        }
    }

    private void generatePromptSchemas(MCPConfiguration config, Path schemaDir) throws IOException {
        Path promptsDir = schemaDir.resolve("prompts");
        Files.createDirectories(promptsDir);
        
        JsonObject promptsConfig = config.toJson().getJsonObject("prompts", new JsonObject());
        
        for (String promptName : promptsConfig.fieldNames()) {
            JsonObject promptDef = promptsConfig.getJsonObject(promptName);
            
            JsonObject promptSchema = new JsonObject()
                .put("$schema", "http://json-schema.org/draft-07/schema#")
                .put("type", "object")
                .put("title", "MCP Prompt: " + promptName)
                .put("description", promptDef.getString("description", ""))
                .put("properties", new JsonObject()
                    .put("name", new JsonObject()
                        .put("type", "string")
                        .put("const", promptName))
                    .put("arguments", new JsonObject()
                        .put("type", "object")
                        .put("description", "Prompt arguments")))
                .put("required", new JsonArray().add("name"));
            
            writeJsonFile(promptsDir.resolve(promptName + ".json"), promptSchema);
        }
    }

    private void generateMcpSchema(MCPConfiguration config, Path schemaDir) throws IOException {
        JsonObject mcpSchema = new JsonObject()
            .put("$schema", "http://json-schema.org/draft-07/schema#")
            .put("type", "object")
            .put("title", "MCP Server Configuration")
            .put("description", "Complete schema for this MCP server")
            .put("properties", new JsonObject()
                .put("tools", new JsonObject()
                    .put("type", "object")
                    .put("description", "Available tools"))
                .put("resources", new JsonObject()
                    .put("type", "object")
                    .put("description", "Available resources"))
                .put("prompts", new JsonObject()
                    .put("type", "object")
                    .put("description", "Available prompts")));
        
        // Add component counts
        JsonObject configJson = config.toJson();
        JsonObject metadata = new JsonObject()
            .put("totalTools", configJson.getJsonObject("tools", new JsonObject()).size())
            .put("totalResources", configJson.getJsonObject("resources", new JsonObject()).size())
            .put("totalPrompts", configJson.getJsonObject("prompts", new JsonObject()).size())
            .put("generatedAt", java.time.Instant.now().toString());
        
        mcpSchema.put("metadata", metadata);
        mcpSchema.put("configuration", configJson);
        
        writeJsonFile(schemaDir.resolve("mcp-server.json"), mcpSchema);
    }

    private void writeJsonFile(Path filePath, JsonObject jsonObject) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(jsonObject.encodePrettily());
        }
    }
}