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
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
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

    @Value("${custom.request.max-content-length:1024000}")
    private int maxContentLength;

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
    public SimpleResult<String> filterApi(HttpServletRequest request, @RequestBody ApiFilterVo apiParam) {
        SimpleResult<String> result = SimpleResult.error("OpenAPI process error");
        String content = apiParam.getOpenAPI();
        SwaggerParseResult parseResult = new OpenAPIParser().readContents(content, null, new ParseOptions());
        OpenAPI openAPI = parseResult.getOpenAPI();
        boolean validOpenApi = openAPI != null;
        if (validOpenApi) {
            if (!CollectionUtils.isEmpty(apiParam.getOperationIds())) {
                openAPI = OpenAPIFilterUtils.filterByOperationIds(openAPI, apiParam.getOperationIds());
                content = SchemaJsonUtils.toJson(openAPI, SchemaJsonUtils.isV31(openAPI));
            }
            if (OpenAPIFilterUtils.isApiContentExceeded(content, maxContentLength)) {
                // 用content生成一个当前服务器的https://xxx.com/openApi/${md5(content)}.json地址，文件存在临时文件夹中访问时可以获取
                result = OpenAPIFilterUtils.generateOpenUrl(content, request);
            } else {
                result = SimpleResult.ok(content);
            }
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


    /**
     * 调试API
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(value = "/openApi/{apiFile}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String openApiFile(@PathVariable("apiFile") String apiFile) throws IOException {
        File file = new File(OpenAPIFilterUtils.getApiTempDir(), apiFile);
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8.name());
    }

}
