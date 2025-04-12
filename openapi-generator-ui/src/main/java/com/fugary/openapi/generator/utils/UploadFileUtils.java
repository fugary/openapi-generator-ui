package com.fugary.openapi.generator.utils;

import com.fugary.openapi.generator.vo.ApiParamVo;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UploadFileUtils {

    /**
     * 获取上传文件信息
     *
     * @param request
     * @return
     */
    public static List<MultipartFile> getUploadFiles(HttpServletRequest request) {
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            List<MultipartFile> files = multipartRequest.getFiles("files");
            if (CollectionUtils.isEmpty(files)) {
                files = multipartRequest.getFiles("file");
            }
            return files;
        }
        return new ArrayList<>();
    }

    /**
     * 计算认证信息
     *
     * @param apiParam
     * @return
     */
    public static List<AuthorizationValue> calcAuthorizationValue(ApiParamVo apiParam) {
        // 配置 Basic Auth 认证信息
        if (apiParam.isAuth() && StringUtils.isNotBlank(apiParam.getUserName()) && StringUtils.isNotBlank(apiParam.getPassword())) {
            String credentials = apiParam.getUserName() + ":" + apiParam.getPassword();
            String base64Credentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            // 创建 AuthorizationValue 对象
            AuthorizationValue auth = new AuthorizationValue()
                    .keyName("Authorization") // 头字段名称
                    .value("Basic " + base64Credentials) // Basic Auth 格式
                    .type("header"); // 指定为请求头
        }
        return new ArrayList<>();
    }

    public static String getFileContent(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Read file error", e);
            return null;
        }
    }
}
