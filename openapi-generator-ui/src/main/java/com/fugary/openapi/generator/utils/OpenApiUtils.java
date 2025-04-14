package com.fugary.openapi.generator.utils;

import com.fugary.openapi.generator.constants.SystemConstants;
import com.fugary.openapi.generator.vo.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Gary.Fu
 * @date 2025/4/13
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OpenApiUtils {

    /**
     * 解析成现实对象
     *
     * @param openAPI
     * @return
     */
    public static List<ApiTagVo> toTags(OpenAPI openAPI) {
        if (openAPI != null) {
            Map<String, List<ApiOperationVo>> pathMap = openAPI.getPaths().entrySet().stream().flatMap(entry -> {
                PathItem pathItem = entry.getValue();
                List<Pair<String, Operation>> operations = getAllOperationsInAPath(pathItem);
                return operations.stream().map(pair -> OpenApiUtils.toOperation(entry.getKey(), pair.getLeft(), pair.getRight()));
            }).collect(Collectors.groupingBy(ApiOperationVo::getTag, LinkedHashMap::new, Collectors.toList()));
            return pathMap.entrySet().stream().map(entry -> {
                ApiTagVo tagVo = createTagVo(openAPI, entry.getKey());
                tagVo.setOperations(entry.getValue());
                return tagVo;
            }).toList();
        }
        return new ArrayList<>();
    }

    public static List<Pair<String, Operation>> getAllOperationsInAPath(PathItem pathObj) {
        List<Pair<String, Operation>> operations = new ArrayList<>();
        addToOperationsList(operations, pathObj.getGet(), PathItem.HttpMethod.GET.name());
        addToOperationsList(operations, pathObj.getPut(), PathItem.HttpMethod.PUT.name());
        addToOperationsList(operations, pathObj.getPost(), PathItem.HttpMethod.POST.name());
        addToOperationsList(operations, pathObj.getPatch(), PathItem.HttpMethod.PATCH.name());
        addToOperationsList(operations, pathObj.getDelete(), PathItem.HttpMethod.DELETE.name());
        addToOperationsList(operations, pathObj.getTrace(), PathItem.HttpMethod.TRACE.name());
        addToOperationsList(operations, pathObj.getOptions(), PathItem.HttpMethod.OPTIONS.name());
        addToOperationsList(operations, pathObj.getHead(), PathItem.HttpMethod.HEAD.name());
        return operations;
    }

    public static void addToOperationsList(List<Pair<String, Operation>> operationsList, Operation operation, String method) {
        if (operation == null) {
            return;
        }
        operationsList.add(Pair.of(method, operation));
    }

    public static Tag findTag(OpenAPI openAPI, String tag) {
        for (Tag apiTag : openAPI.getTags()) {
            if (StringUtils.equalsIgnoreCase(tag, apiTag.getName())) {
                return apiTag;
            }
        }
        return null;
    }

    public static ApiTagVo createTagVo(OpenAPI openAPI, String tag) {
        ApiTagVo apiTagVo = new ApiTagVo();
        Tag apiTag = findTag(openAPI, tag);
        apiTagVo.setName(tag);
        apiTagVo.setTagId(DigestUtils.md2Hex(tag));
        if (apiTag != null) {
            apiTagVo.setDescription(apiTag.getDescription());
        }
        return apiTagVo;
    }

    public static ApiOperationVo toOperation(String path, String method, Operation operation) {
        ApiOperationVo apiOperationVo = new ApiOperationVo();
        apiOperationVo.setTag(operation.getTags().getFirst());
        apiOperationVo.setPath(path);
        apiOperationVo.setMethod(method);
        apiOperationVo.setOperationId(operation.getOperationId());
        apiOperationVo.setSummary(operation.getSummary());
        apiOperationVo.setDescription(operation.getDescription());
        apiOperationVo.setDeprecated(operation.getDeprecated());
        return apiOperationVo;
    }

    /**
     * 复制属性
     *
     * @param from
     * @param to
     * @return
     * @param <T>
     * @param <S>
     */
    public static <T, S> T copy(S from, T to) {
        try {
            BeanUtils.copyProperties(from, to);
        } catch (Exception e) {
            log.error("copy属性错误", e);
        }
        return to;
    }

    /**
     * 复制属性
     *
     * @param from
     * @param to
     * @return
     * @param <T>
     * @param <S>
     */
    public static <T, S> T copy(S from, Class<T> to) {
        if (from == null) {
            return null;
        }
        Constructor<T> constructor = null;
        T target = null;
        try {
            constructor = to.getConstructor();
            target = constructor.newInstance();
            copy(from, target);
        } catch (Exception e) {
            log.error("copy属性错误", e);
        }
        return target;
    }

    /**
     * 解析成ApiParamsVo
     *
     * @param request
     * @return
     */
    public static ApiParamsVo toApiParams(HttpServletRequest request) {
        ApiParamsVo apiParams = new ApiParamsVo();
        String pathPrefix = request.getContextPath() + "/proxy(/.*)";
        String requestPath = request.getRequestURI();
        Matcher matcher = Pattern.compile(pathPrefix).matcher(requestPath);
        if (matcher.matches()) {
            requestPath = matcher.group(1);
        }
        String targetUrl = request.getHeader(SystemConstants.SIMPLE_API_TARGET_URL_HEADER);
        if (StringUtils.startsWith(targetUrl, "//")) { // 没有协议
            targetUrl = request.getScheme() + ":" + targetUrl;
        }
        apiParams.setTargetUrl(targetUrl);
        apiParams.setRequestPath(requestPath);
        apiParams.setMethod(request.getMethod());
        Enumeration<String> headerNames = request.getHeaderNames();
        List<NameValue> headers = apiParams.getHeaderParams();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            boolean excludeHeader = getExcludeHeaders().contains(headerName.toLowerCase());
            if (!excludeHeader) {
                excludeHeader = isExcludeHeaders(headerName.toLowerCase());
            }
            if (!excludeHeader && StringUtils.isNotBlank(headerValue)) {
                headers.add(new NameValue(headerName, headerValue));
            }
        }
        Enumeration<String> parameterNames = request.getParameterNames();
        List<NameValue> parameters = apiParams.getRequestParams();
        List<NameValue> formUrlencoded = apiParams.getFormUrlencoded();
        List<NameValueObj> formData = apiParams.getFormData();
        boolean isUrlencoded = isCompatibleWith(request, MediaType.APPLICATION_FORM_URLENCODED);
        boolean isFormData = isCompatibleWith(request, MediaType.MULTIPART_FORM_DATA);
        if (isFormData) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            multipartRequest.getFileNames().forEachRemaining(fieldName -> {
                formData.add(new NameValueObj(fieldName, multipartRequest.getFiles(fieldName)));
            });
            multipartRequest.getParameterMap().keySet().forEach(paramName -> {
                String paramValue = multipartRequest.getParameter(paramName);
                if (StringUtils.isNotBlank(paramValue)) {
                    formData.add(new NameValueObj(paramName, paramValue));
                }
            });
        } else if (isUrlencoded) {
            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                String parameterValue = request.getParameter(parameterName);
                if (StringUtils.isNotBlank(parameterValue)) {
                    formUrlencoded.add(new NameValue(parameterName, parameterValue));
                }
            }
        } else {
            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                String parameterValue = request.getParameter(parameterName);
                if (StringUtils.isNotBlank(parameterValue)) {
                    parameters.add(new NameValue(parameterName, parameterValue));
                }
            }
        }
        apiParams.setContentType(request.getContentType());
        try {
            apiParams.setRequestBody(StreamUtils.copyToString(getBodyResource(request).getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Body解析错误", e);
        }
        return apiParams;
    }

    /**
     * 过滤部分header请求
     * @return
     */
    public static Set<String> getExcludeHeaders(){
        List<String> list = Arrays.asList(
                HttpHeaders.HOST.toLowerCase(),
                HttpHeaders.ORIGIN.toLowerCase(),
                HttpHeaders.REFERER.toLowerCase()
        );
        return new HashSet<>(list);
    }

    /**
     * 判断是否是需要过滤
     * @return
     */
    public static boolean isExcludeHeaders(String headerName){
        headerName = StringUtils.trimToEmpty(headerName).toLowerCase();
        return getExcludeHeaders().contains(headerName)
                || headerName.matches("^(sec-|simple-).*");
    }

    /**
     * 清理cors相关的头信息，代理时使用自己的头信息
     * @param response ResponseEntity
     */
    public static <T> ResponseEntity<T> removeProxyHeaders(ResponseEntity<T> response) {
        if (response != null) {
            HttpHeaders headers = new HttpHeaders();
            response.getHeaders().forEach((headerName, value) -> {
                if (!StringUtils.startsWithIgnoreCase(headerName, "access-control-")
                        && !StringUtils.equalsIgnoreCase(HttpHeaders.CONNECTION, headerName)) {
                    headers.addAll(headerName, value);
                }
            });
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        }
        return response;
    }

    /**
     * 判断MediaType
     * @param request
     * @param matchTypes
     * @return
     */
    public static boolean isCompatibleWith(HttpServletRequest request, MediaType...matchTypes) {
        List<MediaType> mediaTypes = MediaType.parseMediaTypes(request.getContentType());
        return isCompatibleWith(mediaTypes, matchTypes);
    }

    /**
     * 判断MediaType
     * @param paramsVo
     * @param matchTypes
     * @return
     */
    public static boolean isCompatibleWith(ApiParamsVo paramsVo, MediaType...matchTypes) {
        List<MediaType> mediaTypes = MediaType.parseMediaTypes(paramsVo.getContentType());
        return isCompatibleWith(mediaTypes, matchTypes);
    }

    public static boolean isCompatibleWith(List<MediaType> mediaTypes, MediaType...matchTypes) {
        for (MediaType type : mediaTypes) {
            for (MediaType mediaType : matchTypes) {
                if (mediaType.isCompatibleWith(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Resource getBodyResource(HttpServletRequest request) throws IOException {
        Resource bodyResource = new InputStreamResource(request.getInputStream());
        if(request instanceof ContentCachingRequestWrapper){
            ContentCachingRequestWrapper contentCachingRequestWrapper = (ContentCachingRequestWrapper) request;
            if (contentCachingRequestWrapper.getContentAsByteArray().length > 0) {
                bodyResource = new ByteArrayResource(contentCachingRequestWrapper.getContentAsByteArray());
            }
        }
        return bodyResource;
    }
}
