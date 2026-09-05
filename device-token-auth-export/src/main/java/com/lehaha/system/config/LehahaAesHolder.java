package com.lehaha.system.config;

import com.lehaha.common.utils.AES.AesUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class LehahaAesHolder {
    @Value("${ }")
    private String key;

    @Value("${ }")
    private String iv;

    @PostConstruct
    public void validateConfig() {
        AesUtil.validateKeyAndIv(key, iv);
    }

    public String decrypt(String cipherBase64) {
        return AesUtil.decrypt(cipherBase64, key, iv);
    }

    public String encrypt(String plainText) {
        return AesUtil.encrypt(plainText, key, iv);
    }
}
