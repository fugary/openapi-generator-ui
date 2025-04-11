package com.fugary.openapi.generator.vo;

import com.fugary.openapi.generator.constants.SystemConstants;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 2025/4/11 9:38 .<br>
 *
 * @author gary.fu
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class SimpleResult<T> {

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应的数据
     */
    private T resultData;

    /**
     * 附加信息
     */
    private Map<String, Serializable> addons;

    /**
     * 成功输出
     *
     * @return
     */
    public static <T> SimpleResult<T> ok(T resultData) {
        return SimpleResult.<T>builder().resultData(resultData)
                .code(SystemConstants.SUCCESS)
                .build();
    }

    /**
     * 失败输出输出
     *
     * @return
     */
    public static <T> SimpleResult<T> error(String message) {
        return SimpleResult.<T>builder()
                .code(SystemConstants.ERROR)
                .message(message)
                .build();
    }

    /**
     * 失败输出输出
     *
     * @return
     */
    public static <T> SimpleResult<T> error(T resultData, String message) {
        return SimpleResult.<T>builder().resultData(resultData)
                .code(SystemConstants.ERROR)
                .message(message)
                .build();
    }

    /**
     * 附加信息
     *
     * @param key
     * @param value
     * @return
     */
    public SimpleResult<T> add(String key, Serializable value) {
        addons = addons == null ? new HashMap<>() : addons;
        addons.put(key, value);
        return this;
    }

    /**
     * 是否成功
     *
     * @return
     */
    public boolean isSuccess() {
        return code == SystemConstants.SUCCESS;
    }
}
