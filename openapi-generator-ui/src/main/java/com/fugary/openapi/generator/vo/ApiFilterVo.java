package com.fugary.openapi.generator.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Create date 2025/4/14<br>
 *
 * @author gary.fu
 */
@Data
public class ApiFilterVo implements Serializable {
    private String openAPI;
    private List<String> operationIds;
}
