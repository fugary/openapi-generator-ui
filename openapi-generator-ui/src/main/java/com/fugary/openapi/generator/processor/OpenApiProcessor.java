package com.fugary.openapi.generator.processor;

import com.fugary.openapi.generator.vo.ApiParamVo;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author fufuguo
 */
public interface OpenApiProcessor {
    /**
     * 解析内容
     *
     * @param apiParam
     * @param file
     * @return
     */
    OpenAPI process(ApiParamVo apiParam, MultipartFile file);
}
