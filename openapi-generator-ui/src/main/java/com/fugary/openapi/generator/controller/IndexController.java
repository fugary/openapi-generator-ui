package com.fugary.openapi.generator.controller;

import com.fugary.openapi.generator.vo.SimpleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Create date 2025/4/11<br>
 *
 * @author gary.fu
 */
@RequestMapping("/")
@Controller
@Slf4j
public class IndexController {

    @GetMapping(path = {"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/open-api-view")
    public String openApiView() {
        return "openapi-view";
    }

    @ResponseBody
    @GetMapping("/test")
    public SimpleResult<String> test(){
        return SimpleResult.ok("ok");
    }
}
