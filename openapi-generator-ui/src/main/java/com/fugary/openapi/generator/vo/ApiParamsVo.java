package com.fugary.openapi.generator.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库中存储的数据解析出来
 * Create date 2024/7/17<br>
 *
 * @author gary.fu
 */
@Data
public class ApiParamsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = -7676986301038603213L;
    private List<NameValue> pathParams = new ArrayList<>();
    private List<NameValue> requestParams = new ArrayList<>();
    private List<NameValue> headerParams = new ArrayList<>();
    private List<NameValue> formUrlencoded = new ArrayList<>();
    private List<NameValueObj> formData = new ArrayList<>();
    private String contentType;
    private String requestBody;
    private String requestFormat;
    private String authType;
    private String authBody;
    // ==================group信息==================
    private String targetUrl;
    // ==================request信息==================
    private String requestPath;
    private String method;
}
