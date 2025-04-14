package com.fugary.openapi.generator.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author Gary.Fu
 * @date 2025/4/13
 */
@Data
public class ApiTagVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String tagId;
    private String name;
    private String description;
    private List<ApiOperationVo> operations;
}
