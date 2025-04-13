package com.fugary.openapi.generator.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Gary.Fu
 * @date 2025/4/13
 */
@Data
public class ApiOperationVo implements Serializable {
    private String tag;
    private String method;
    private String path;
    private String summary = null;
    private String description = null;
    private String operationId;
}
