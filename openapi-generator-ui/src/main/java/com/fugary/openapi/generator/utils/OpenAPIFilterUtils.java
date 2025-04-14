package com.fugary.openapi.generator.utils;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAPI 过滤工具类，根据 operationIds 过滤端点和相关 schemas。
 *
 * @author Gary.Fu
 * @date 2025/4/13
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OpenAPIFilterUtils {

    /**
     * 根据 operationIds 过滤 OpenAPI 规范
     *
     * @param openAPI      OpenAPI 对象
     * @param operationIds 要保留的 operationId 列表
     * @return 过滤后的 OpenAPI 对象
     */
    public static OpenAPI filterByOperationIds(OpenAPI openAPI, List<String> operationIds) {
        if (openAPI == null || operationIds == null) {
            throw new IllegalArgumentException("OpenAPI 或 operationIds 不能为空");
        }
        // 过滤 paths 和 tags
        OpenAPI filteredOpenAPI = filterPaths(openAPI, new HashSet<>(operationIds));

        // 过滤 components.schemas
        filterSchemas(filteredOpenAPI);

        return filteredOpenAPI;
    }

    /**
     * 过滤 paths，仅保留指定 operationIds 的端点，并处理 tags
     */
    private static OpenAPI filterPaths(OpenAPI openAPI, Set<String> operationIds) {
        OpenAPI result = new OpenAPI()
                .info(openAPI.getInfo())
                .servers(openAPI.getServers())
                .components(openAPI.getComponents() != null ? openAPI.getComponents() : new Components());

        Paths filteredPaths = new Paths();
        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            PathItem pathItem = entry.getValue();
            PathItem filteredPathItem = new PathItem();

            // 过滤每个 HTTP 方法
            if (pathItem.getGet() != null && operationIds.contains(pathItem.getGet().getOperationId())) {
                filteredPathItem.setGet(safeCopyOperation(pathItem.getGet()));
            }
            if (pathItem.getPost() != null && operationIds.contains(pathItem.getPost().getOperationId())) {
                filteredPathItem.setPost(safeCopyOperation(pathItem.getPost()));
            }
            if (pathItem.getPut() != null && operationIds.contains(pathItem.getPut().getOperationId())) {
                filteredPathItem.setPut(safeCopyOperation(pathItem.getPut()));
            }
            if (pathItem.getDelete() != null && operationIds.contains(pathItem.getDelete().getOperationId())) {
                filteredPathItem.setDelete(safeCopyOperation(pathItem.getDelete()));
            }
            if (pathItem.getPatch() != null && operationIds.contains(pathItem.getPatch().getOperationId())) {
                filteredPathItem.setPatch(safeCopyOperation(pathItem.getPatch()));
            }

            // 如果有保留的操作，添加到路径
            if (!filteredPathItem.readOperations().isEmpty()) {
                filteredPaths.put(entry.getKey(), filteredPathItem);
            }
        }

        result.setPaths(filteredPaths.isEmpty() ? new Paths() : filteredPaths);
        filterTags(openAPI, result);
        return result;
    }

    /**
     * 安全复制操作，处理 tags
     */
    private static io.swagger.v3.oas.models.Operation safeCopyOperation(io.swagger.v3.oas.models.Operation operation) {
        io.swagger.v3.oas.models.Operation copy = new io.swagger.v3.oas.models.Operation()
                .operationId(operation.getOperationId())
                .summary(operation.getSummary())
                .description(operation.getDescription())
                .parameters(operation.getParameters())
                .requestBody(operation.getRequestBody())
                .responses(operation.getResponses())
                .deprecated(operation.getDeprecated())
                .security(operation.getSecurity())
                .extensions(operation.getExtensions());

        // 处理操作级 tags
        List<String> tags = operation.getTags();
        copy.setTags(tags != null ? new ArrayList<>(tags) : new ArrayList<>());
        return copy;
    }

    /**
     * 过滤顶层 tags，仅保留过滤后操作引用的标签
     */
    private static void filterTags(OpenAPI openAPI, OpenAPI result) {
        // 从过滤后的 paths 收集 tags
        Set<String> usedTags = result.getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .filter(operation -> operation.getTags() != null)
                .flatMap(operation -> operation.getTags().stream())
                .collect(Collectors.toSet());

        // 过滤顶层 tags
        if (openAPI.getTags() != null && !usedTags.isEmpty()) {
            List<io.swagger.v3.oas.models.tags.Tag> filteredTags = openAPI.getTags().stream()
                    .filter(tag -> usedTags.contains(tag.getName()))
                    .map(tag -> new io.swagger.v3.oas.models.tags.Tag()
                            .name(tag.getName())
                            .description(tag.getDescription())
                            .extensions(tag.getExtensions()))
                    .collect(Collectors.toList());
            result.setTags(filteredTags);
        } else {
            result.setTags(new ArrayList<>());
        }
    }

    /**
     * 过滤 components.schemas，仅保留与路径相关的模型
     */
    private static void filterSchemas(OpenAPI openAPI) {
        if (openAPI.getPaths().isEmpty() || openAPI.getComponents() == null) {
            openAPI.setComponents(new Components());
            return;
        }

        // 收集所有相关 schema 名称
        Set<String> requiredSchemas = collectRequiredSchemas(openAPI);

        // 过滤 schemas
        Map<String, Schema> filteredSchemas = new HashMap<>();
        if (openAPI.getComponents().getSchemas() != null) {
            for (String schemaName : requiredSchemas) {
                Schema schema = openAPI.getComponents().getSchemas().get(schemaName);
                if (schema != null) {
                    filteredSchemas.put(schemaName, schema);
                }
            }
        }

        openAPI.getComponents().setSchemas(filteredSchemas);
    }

    /**
     * 收集路径中引用的所有 schema 名称（包括 $ref、allOf 等）
     */
    private static Set<String> collectRequiredSchemas(OpenAPI openAPI) {
        Set<String> schemaNames = new HashSet<>();
        Set<String> visitedSchemas = new HashSet<>(); // 防止循环引用

        // 遍历所有路径和操作
        for (PathItem pathItem : openAPI.getPaths().values()) {
            for (io.swagger.v3.oas.models.Operation operation : pathItem.readOperations()) {
                // 检查 requestBody
                if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                    operation.getRequestBody().getContent().values().stream()
                            .map(MediaType::getSchema)
                            .filter(Objects::nonNull)
                            .forEach(schema -> collectSchemaNames(schema, schemaNames, visitedSchemas, openAPI));
                }

                // 检查 responses
                if (operation.getResponses() != null) {
                    for (ApiResponse response : operation.getResponses().values()) {
                        if (response.getContent() != null) {
                            response.getContent().values().stream()
                                    .map(MediaType::getSchema)
                                    .filter(Objects::nonNull)
                                    .forEach(schema -> collectSchemaNames(schema, schemaNames, visitedSchemas, openAPI));
                        }
                    }
                }
            }
        }

        return schemaNames;
    }

    /**
     * 递归收集 schema 名称（支持 $ref、allOf、oneOf、anyOf），包括子属性
     */
    private static void collectSchemaNames(Schema schema, Set<String> schemaNames, Set<String> visitedSchemas, OpenAPI openAPI) {
        if (schema == null) {
            return;
        }

        // 处理 $ref
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            if (schemaNames.add(schemaName) && !visitedSchemas.contains(schemaName)) {
                visitedSchemas.add(schemaName);
                // 获取引用的 schema
                if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
                    Schema refSchema = openAPI.getComponents().getSchemas().get(schemaName);
                    if (refSchema != null) {
                        collectSchemaNames(refSchema, schemaNames, visitedSchemas, openAPI);
                    }
                }
            }
        }

        // 处理 allOf
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(s -> collectSchemaNames((Schema) s, schemaNames, visitedSchemas, openAPI));
        }

        // 处理 oneOf
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(s -> collectSchemaNames((Schema) s, schemaNames, visitedSchemas, openAPI));
        }

        // 处理 anyOf
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(s -> collectSchemaNames((Schema) s, schemaNames, visitedSchemas, openAPI));
        }

        // 处理 properties
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(s -> collectSchemaNames((Schema) s, schemaNames, visitedSchemas, openAPI));
        }

        // 处理数组的 items
        if (schema instanceof ArraySchema && ((ArraySchema) schema).getItems() != null) {
            collectSchemaNames(((ArraySchema) schema).getItems(), schemaNames, visitedSchemas, openAPI);
        }

        // 处理嵌套对象
        if (schema instanceof ObjectSchema && schema.getProperties() != null) {
            schema.getProperties().values().forEach(s -> collectSchemaNames((Schema) s, schemaNames, visitedSchemas, openAPI));
        }
    }

}
