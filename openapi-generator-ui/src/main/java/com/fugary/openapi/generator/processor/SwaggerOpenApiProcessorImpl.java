package com.fugary.openapi.generator.processor;

import com.fugary.openapi.generator.constants.SystemConstants;
import com.fugary.openapi.generator.utils.UploadFileUtils;
import com.fugary.openapi.generator.vo.ApiParamVo;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author fufuguo
 */
@Component
public class SwaggerOpenApiProcessorImpl implements OpenApiProcessor, InitializingBean {

    @Override
    public OpenAPI process(ApiParamVo apiParam, MultipartFile file) {
        ParseOptions parseOptions = new ParseOptions();
//        parseOptions.setResolveFully(true);
        SwaggerParseResult result = null;
        switch (apiParam.getType()) {
            case SystemConstants.TYPE_URL:
                result = new OpenAPIParser().readLocation(apiParam.getUrl(), UploadFileUtils.calcAuthorizationValue(apiParam), parseOptions);
                return result.getOpenAPI();
            case SystemConstants.TYPE_CONTENT:
                result = new OpenAPIParser().readContents(apiParam.getContent(), null, parseOptions);
                return result.getOpenAPI();
            case SystemConstants.TYPE_FILE:
                String content = UploadFileUtils.getFileContent(file);
                result = new OpenAPIParser().readContents(content, null, parseOptions);
                return result.getOpenAPI();
        }
        return null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.setProperty("swagger.parser.http.client.trustAllCertificates", "true");
    }
}
