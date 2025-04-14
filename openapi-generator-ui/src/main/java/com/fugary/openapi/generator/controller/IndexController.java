package com.fugary.openapi.generator.controller;

import com.fugary.openapi.generator.processor.ApiInvokeProcessor;
import com.fugary.openapi.generator.processor.OpenApiProcessor;
import com.fugary.openapi.generator.utils.OpenAPIFilterUtils;
import com.fugary.openapi.generator.utils.OpenApiUtils;
import com.fugary.openapi.generator.utils.SchemaJsonUtils;
import com.fugary.openapi.generator.utils.UploadFileUtils;
import com.fugary.openapi.generator.vo.ApiFilterVo;
import com.fugary.openapi.generator.vo.ApiParamVo;
import com.fugary.openapi.generator.vo.SimpleResult;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.List;

/**
 * Create date 2025/4/11<br>
 *
 * @author gary.fu
 */
@RequestMapping("/")
@Controller
@Slf4j
public class IndexController {

    @Autowired
    private OpenApiProcessor openApiProcessor;

    @Autowired
    private ApiInvokeProcessor apiInvokeProcessor;

    @GetMapping(path = {"/", "/index"})
    public String index() {
        return "index";
    }

    @ResponseBody
    @PostMapping("loadApi")
    public SimpleResult<String> loadApi(@ModelAttribute ApiParamVo apiParam, HttpServletRequest request) {
        List<MultipartFile> uploadFiles = UploadFileUtils.getUploadFiles(request);
        MultipartFile multipartFile = uploadFiles.isEmpty() ? null : uploadFiles.getFirst();
        OpenAPI openAPI = openApiProcessor.process(apiParam, multipartFile);
        SimpleResult<String> result = SimpleResult.error("OpenAPI process error");
        if (openAPI != null) {
            result = SimpleResult.ok(SchemaJsonUtils.toJson(openAPI, SchemaJsonUtils.isV31(openAPI)));
            result.add("apiTags", (Serializable) OpenApiUtils.toTags(openAPI));
        }
        return result;
    }

    @ResponseBody
    @PostMapping("filterApi")
    public SimpleResult<String> filterApi(@ModelAttribute ApiFilterVo apiParam) {
        SimpleResult<String> result = SimpleResult.error("OpenAPI process error");
        SwaggerParseResult parseResult = new OpenAPIParser().readContents(apiParam.getOpenAPI(), null, new ParseOptions());
        OpenAPI openAPI = parseResult.getOpenAPI();
        if (openAPI != null) {
            openAPI = OpenAPIFilterUtils.filterByOperationIds(openAPI, apiParam.getOperationIds());
            result = SimpleResult.ok(SchemaJsonUtils.toJson(openAPI, SchemaJsonUtils.isV31(openAPI)));
        }
        return result;
    }

    /**
     * 调试API
     *
     * @return
     */
    @RequestMapping("/proxy/**")
    public ResponseEntity<?> proxyApi(HttpServletRequest request, HttpServletResponse response) {
        return apiInvokeProcessor.invoke(request, response);
    }

}
