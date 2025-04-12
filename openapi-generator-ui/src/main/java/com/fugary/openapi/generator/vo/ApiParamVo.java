package com.fugary.openapi.generator.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author fufuguo
 */
@Data
public class ApiParamVo implements Serializable {

    private String type;
    private String url;
    private boolean auth;
    private String userName;
    private String password;
    private String content;

}
