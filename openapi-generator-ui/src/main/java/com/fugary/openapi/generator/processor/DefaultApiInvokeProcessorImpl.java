package com.fugary.openapi.generator.processor;

import com.fugary.openapi.generator.utils.OpenApiUtils;
import com.fugary.openapi.generator.vo.ApiParamsVo;
import com.fugary.openapi.generator.vo.NameValue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Create date 2024/7/17<br>
 *
 * @author gary.fu
 */
@Slf4j
@Component
public class DefaultApiInvokeProcessorImpl implements ApiInvokeProcessor, InitializingBean {

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private RestTemplate restTemplate;


    @Override
    public <T> ResponseEntity<T> invoke(HttpServletRequest request, HttpServletResponse response, Class<T> clazz) {
        return invoke(OpenApiUtils.toApiParams(request), clazz);
    }

    @Override
    public <T> ResponseEntity<T> invoke(ApiParamsVo mockParams, Class<T> clazz) {
        String requestUrl = getRequestUrl(mockParams.getTargetUrl(), mockParams);
        HttpEntity<?> entity = new HttpEntity<>(mockParams.getRequestBody(), getHeaders(mockParams));
        if (OpenApiUtils.isCompatibleWith(mockParams, MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_FORM_URLENCODED)) {
            entity = new HttpEntity<>(getMultipartBody(mockParams), getHeaders(mockParams));
        }
        try {
            ResponseEntity<T> responseEntity = restTemplate.exchange(requestUrl,
                    Optional.of(HttpMethod.valueOf(mockParams.getMethod())).orElse(HttpMethod.GET),
                    entity, clazz);
            responseEntity = processRedirect(responseEntity, mockParams, entity, clazz);
            return OpenApiUtils.removeProxyHeaders(responseEntity);
        } catch (HttpClientErrorException e) {
            return OpenApiUtils.removeProxyHeaders(ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAs(clazz)));
        } catch (Exception e) {
            log.error("获取数据错误", e);
        }
        return ResponseEntity.notFound().build();
    }

    protected <T> ResponseEntity<T> processRedirect(ResponseEntity<T> responseEntity,
                                                    ApiParamsVo mockParams,
                                                    HttpEntity<?> entity, Class<T> clazz) {
        HttpStatusCode httpStatus = responseEntity.getStatusCode();
        if (httpStatus.is3xxRedirection()) {
            URI location = responseEntity.getHeaders().getLocation();
            if (location != null) {
                URI targetUri = UriComponentsBuilder.fromUri(location)
                        .queryParams(getQueryParams(mockParams))
                        .build(true).toUri();
                responseEntity = restTemplate.exchange(targetUri,
                        Optional.of(HttpMethod.valueOf(mockParams.getMethod())).orElse(HttpMethod.GET),
                        entity, clazz);
            }
        }
        return responseEntity;
    }

    protected MultiValueMap<String, Object> getMultipartBody(ApiParamsVo mockParams) {
        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        mockParams.getFormData().forEach(nv -> {
            if (nv.getValue() instanceof Iterable) {
                ((Iterable) nv.getValue()).forEach(v -> bodyMap.add(nv.getName(), ((MultipartFile) v).getResource()));
            } else {
                bodyMap.add(nv.getName(), nv.getValue());
            }
        });
        mockParams.getFormUrlencoded().forEach(nv -> {
            bodyMap.add(nv.getName(), nv.getValue());
        });
        return bodyMap;
    }

    /**
     * 计算请求url地址
     *
     * @param baseUrl
     * @param mockParams
     * @return
     */
    protected String getRequestUrl(String baseUrl, ApiParamsVo mockParams) {
        String requestUrl = mockParams.getRequestPath();
        requestUrl = requestUrl.replaceAll(":([\\w-]+)", "{$1}"); // spring 支持的ant path不支持:var格式，只支持{var}格式
        requestUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .path(requestUrl)
                .queryParams(getQueryParams(mockParams))
                .build(true).toUriString();
        for (NameValue nv : mockParams.getPathParams()) {
            requestUrl = requestUrl.replace("{" + nv.getName() + "}", nv.getValue());
        }
        return requestUrl;
    }

    /**
     * 获取头信息
     *
     * @param paramsVo
     * @return
     */
    protected HttpHeaders getHeaders(ApiParamsVo paramsVo) {
        HttpHeaders headers = new HttpHeaders();
        paramsVo.getHeaderParams().forEach(nv -> {
            if (StringUtils.equalsIgnoreCase(nv.getName(), HttpHeaders.ACCEPT_ENCODING)) {
                nv.setValue("gzip, deflate");
            }
            headers.addIfAbsent(nv.getName(), nv.getValue());
        });
        return headers;
    }

    /**
     * 获取参数
     *
     * @param paramsVo
     * @return
     */
    protected MultiValueMap<String, String> getQueryParams(ApiParamsVo paramsVo) {
        return new MultiValueMapAdapter<>(paramsVo.getRequestParams().stream()
                .collect(Collectors.toMap(NameValue::getName, nv -> List.of(nv.getValue()))));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.restTemplate = restTemplateBuilder.requestFactory(HttpComponentsClientHttpRequestFactory.class).build();
    }
}
