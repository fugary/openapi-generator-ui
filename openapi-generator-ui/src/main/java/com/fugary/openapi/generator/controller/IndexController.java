package com.fugary.openapi.generator.controller;

import com.fugary.openapi.generator.processor.ApiInvokeProcessor;
import com.fugary.openapi.generator.processor.OpenApiProcessor;
import com.fugary.openapi.generator.utils.OpenApiUtils;
import com.fugary.openapi.generator.utils.SchemaJsonUtils;
import com.fugary.openapi.generator.utils.UploadFileUtils;
import com.fugary.openapi.generator.vo.ApiParamVo;
import com.fugary.openapi.generator.vo.SimpleResult;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(path = {"/", "/index"})
    public String indexPost(@ModelAttribute ApiParamVo apiParam, Model model, HttpServletRequest request) {
        model.addAttribute("apiParam", apiParam);
        List<MultipartFile> uploadFiles = UploadFileUtils.getUploadFiles(request);
        MultipartFile multipartFile = uploadFiles.isEmpty() ? null : uploadFiles.getFirst();
        OpenAPI openAPI = openApiProcessor.process(apiParam, multipartFile);
        if (openAPI != null) {
            model.addAttribute("openAPI", SchemaJsonUtils.toJson(openAPI, SchemaJsonUtils.isV31(openAPI)));
        }
        model.addAttribute("apiTags", OpenApiUtils.toTags(openAPI));
        return "index";
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

    @ResponseBody
    @GetMapping("/test")
    public SimpleResult<String> test() {
        return SimpleResult.ok("ok");
    }
}
