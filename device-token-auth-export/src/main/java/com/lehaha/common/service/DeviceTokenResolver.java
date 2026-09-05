package com.lehaha.common.service;

/**
 * 校验 token 合法性，不通过则抛出 ServiceException。
 */
public interface DeviceTokenResolver {
    /**
     * 校验 token 合法性，不通过则抛出 ServiceException。
     *
     * @param token 请求 Header 中的原始 token 字符串
     */
    void validate(String token);
}
