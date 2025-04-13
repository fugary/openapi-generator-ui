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
     * @param openAPI  OpenAPI
     * @param operationIds 要保留的 operationId 列表
     * @return 过滤后的 OpenAPI 对象
     */
    public static OpenAPI filterByOperationIds(OpenAPI openAPI, List<String> operationIds) {
        // 解析 OpenAPI
        if (openAPI == null) {
            throw new IllegalArgumentException("无法解析 OpenAPI 规范");
        }

        // 过滤 paths
        OpenAPI filteredOpenAPI = filterPaths(openAPI, new HashSet<>(operationIds));

        // 过滤 components.schemas
        filterSchemas(filteredOpenAPI);

        return filteredOpenAPI;
    }

    /**
     * 过滤 paths，仅保留指定 operationIds 的端点
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
                filteredPathItem.setGet(pathItem.getGet());
            }
            if (pathItem.getPost() != null && operationIds.contains(pathItem.getPost().getOperationId())) {
                filteredPathItem.setPost(pathItem.getPost());
            }
            if (pathItem.getPut() != null && operationIds.contains(pathItem.getPut().getOperationId())) {
                filteredPathItem.setPut(pathItem.getPut());
            }
            if (pathItem.getDelete() != null && operationIds.contains(pathItem.getDelete().getOperationId())) {
                filteredPathItem.setDelete(pathItem.getDelete());
            }
            if (pathItem.getPatch() != null && operationIds.contains(pathItem.getPatch().getOperationId())) {
                filteredPathItem.setPatch(pathItem.getPatch());
            }

            // 如果有保留的操作，添加到路径
            if (!filteredPathItem.readOperations().isEmpty()) {
                filteredPaths.put(entry.getKey(), filteredPathItem);
            }
        }

        result.setPaths(filteredPaths.isEmpty() ? new Paths() : filteredPaths);
        return result;
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

        // 遍历所有路径和操作
        for (PathItem pathItem : openAPI.getPaths().values()) {
            for (io.swagger.v3.oas.models.Operation operation : pathItem.readOperations()) {
                // 检查 requestBody
                if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                    operation.getRequestBody().getContent().values().stream()
                            .map(MediaType::getSchema)
                            .filter(Objects::nonNull)
                            .forEach(schema -> collectSchemaNames(schema, schemaNames));
                }

                // 检查 responses
                if (operation.getResponses() != null) {
                    for (ApiResponse response : operation.getResponses().values()) {
                        if (response.getContent() != null) {
                            response.getContent().values().stream()
                                    .map(MediaType::getSchema)
                                    .filter(Objects::nonNull)
                                    .forEach(schema -> collectSchemaNames(schema, schemaNames));
                        }
                    }
                }
            }
        }

        return schemaNames;
    }

    /**
     * 递归收集 schema 名称（支持 $ref、allOf、oneOf、anyOf）
     */
    private static void collectSchemaNames(Schema schema, Set<String> schemaNames) {
        if (schema == null) {
            return;
        }

        // 处理 $ref
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            schemaNames.add(schemaName);
        }

        // 处理 allOf
        if (schema.getAllOf() != null) {
            schema.getAllOf().forEach(s -> collectSchemaNames((Schema) s, schemaNames));
        }

        // 处理 oneOf
        if (schema.getOneOf() != null) {
            schema.getOneOf().forEach(s -> collectSchemaNames((Schema) s, schemaNames));
        }

        // 处理 anyOf
        if (schema.getAnyOf() != null) {
            schema.getAnyOf().forEach(s -> collectSchemaNames((Schema) s, schemaNames));
        }

        // 处理 properties（对象类型的字段）
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(s -> collectSchemaNames((Schema) s, schemaNames));
        }

        // 处理数组的 items
        if (schema instanceof ArraySchema && ((ArraySchema) schema).getItems() != null) {
            collectSchemaNames(((ArraySchema) schema).getItems(), schemaNames);
        }

        // 处理嵌套对象
        if (schema instanceof ObjectSchema && schema.getProperties() != null) {
            schema.getProperties().values().forEach(s -> collectSchemaNames((Schema) s, schemaNames));
        }
    }

}
