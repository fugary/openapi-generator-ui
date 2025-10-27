package com.fugary.openapi.generator.controller;

import com.fugary.openapi.generator.constants.SystemConstants;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    @Value("${custom.build.version:1.0.0}")
    private String buildVersion;

    @Value("${custom.adsence.address:}")
    private String adsenceAddress;

    @Value("${custom.alert.content:}")
    private String alertContent;

    @GetMapping(path = {"/", "/index"})
    public String index(Model model) {
        model.addAttribute("buildVersion", buildVersion);
        model.addAttribute("adsenceAddress", adsenceAddress);
        model.addAttribute("alertContent", alertContent);
        return "index";
    }

    @ResponseBody
    @PostMapping("loadApi")
    public SimpleResult<String> loadApi(@ModelAttribute ApiParamVo apiParam, HttpServletRequest request) {
        List<MultipartFile> uploadFiles = UploadFileUtils.getUploadFiles(request);
        MultipartFile multipartFile = uploadFiles.isEmpty() ? null : uploadFiles.getFirst();
        OpenAPI openAPI = openApiProcessor.process(apiParam, multipartFile);
        SimpleResult<String> result = SimpleResult.error(SystemConstants.TYPE_URL.equals(apiParam.getType())
                ? "OpenAPI url load or parse error" : "OpenAPI content parse error");
        if (openAPI != null) {
            result = SimpleResult.ok(SchemaJsonUtils.toJson(openAPI, SchemaJsonUtils.isV31(openAPI)));
            result.add("apiTags", (Serializable) OpenApiUtils.toTags(openAPI));
        }
        return result;
    }

    @ResponseBody
    @PostMapping("filterApi")
    public SimpleResult<String> filterApi(@RequestBody ApiFilterVo apiParam) {
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
    @ResponseBody
    @RequestMapping("/proxy/**")
    public String proxyApi(HttpServletRequest request, HttpServletResponse response) {
        ResponseEntity<String> responseEntity = apiInvokeProcessor.invoke(request, response, String.class);
        MediaType contentType = responseEntity.getHeaders().getContentType();
        if (contentType != null) {
            response.setContentType(contentType.toString());
            response.setStatus(responseEntity.getStatusCode().value());
        }
        return responseEntity.getBody();
    }

}
