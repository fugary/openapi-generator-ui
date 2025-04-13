package com.fugary.openapi.generator.processor;

import com.fugary.openapi.generator.vo.ApiParamsVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

/**
 * Create date 2024/7/17<br>
 *
 * @author gary.fu
 */
public interface ApiInvokeProcessor {

    /**
     * 推送处理器
     *
     * @param mockParams 请求参数
     * @return
     */
    ResponseEntity<byte[]> invoke(ApiParamsVo mockParams);

    /**
     * 请求和相依发送
     *
     * @param request
     * @param response
     * @return
     */
    ResponseEntity<?> invoke(HttpServletRequest request, HttpServletResponse response);
}
