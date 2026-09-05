package com.lehaha.common.utils.AES;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES/CBC/PKCS5Padding；KEY / IV 由配置注入。
 * KEY 长度须为 16 / 24 / 32 字节（UTF-8）；IV 须为 16 字节。
 */
public final class AesUtil {

    private static final Logger log = LoggerFactory.getLogger(AesUtil.class);
    private static final String AES = "AES";
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private AesUtil() {
    }

    public static void validateKeyAndIv(String key, String iv) {
        if (key == null || iv == null) {
            throw new IllegalArgumentException("AES key and iv must not be null");
        }
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        int n = keyBytes.length;
        if (n != 16 && n != 24 && n != 32) {
            throw new IllegalArgumentException("AES key length must be 16, 24 or 32 bytes, got: " + n);
        }
        int ivLen = iv.getBytes(StandardCharsets.UTF_8).length;
        if (ivLen != 16) {
            throw new IllegalArgumentException("AES IV length must be 16 bytes, got: " + ivLen);
        }
    }

    public static String encrypt(String content, String key, String iv) {
        if (content == null) {
            return null;
        }
        validateKeyAndIv(key, iv);
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptBytes = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptBytes);
        } catch (Exception e) {
            log.warn("AES encrypt failed: {}", e.getMessage());
            throw new IllegalStateException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String encryptStr, String key, String iv) {
        if (encryptStr == null || encryptStr.isEmpty()) {
            return null;
        }
        validateKeyAndIv(key, iv);
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] raw = Base64.getDecoder().decode(encryptStr.trim());
            byte[] decryptBytes = cipher.doFinal(raw);
            return new String(decryptBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.debug("AES decrypt failed: {}", e.getMessage());
            return null;
        }
    }
}
