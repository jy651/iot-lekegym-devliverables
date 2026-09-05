package com.lehaha.manage.service.impl;

import com.lehaha.common.exception.ServiceException;
import com.lehaha.common.service.DeviceTokenResolver;
import com.lehaha.common.utils.StringUtils;
import com.lehaha.manage.mapper.EquipmentMapper;
import com.lehaha.system.config.LehahaAesHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DeviceTokenResolverImpl implements DeviceTokenResolver {
    /** token 最大有效期：24 小时 */
    private static final long MAX_TOKEN_AGE_MS = 24L * 60 * 60 * 1000;

    /** Redis 缓存时长：10 分钟 */
    private static final long CACHE_TTL_SECONDS = 600L;

    private static final String REDIS_KEY_PREFIX = "device_token:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private LehahaAesHolder lehahaAesHolder;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Override
    public void validate(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new ServiceException("token 不能为空");
        }

        String normalizedToken = normalizeToken(token);
        String redisKey = REDIS_KEY_PREFIX + normalizedToken;

        // 1. Redis 已缓存 → 已鉴权，放行
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey))) {
            return;
        }

        // 2. AES 解密
        String plain = lehahaAesHolder.decrypt(normalizedToken);
        if (StringUtils.isEmpty(plain)) {
            throw new ServiceException("token 无效");
        }

        // 3. 解析明文：equipmentId-categoryId-timestamp
        String[] parts = plain.split("-");
        if (parts.length < 3) {
            throw new ServiceException("token 格式错误");
        }
        long equipmentId;
        long categoryId;
        long ts;
        try {
            equipmentId = Long.parseLong(parts[0].trim());
            categoryId = Long.parseLong(parts[1].trim());
            ts = Long.parseLong(parts[parts.length - 1].trim());
        } catch (NumberFormatException e) {
            throw new ServiceException("token 内容格式错误");
        }

        // 4. 时间戳校验
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > MAX_TOKEN_AGE_MS) {
            throw new ServiceException("token 已过期");
        }

        // 5. 校验 equipmentId + categoryId 是否在 t_equipment 中匹配
        if (equipmentMapper.countByIdAndCategory(equipmentId, categoryId) <= 0) {
            throw new ServiceException("设备与分类不匹配");
        }

        // 6. 校验通过，缓存到 Redis
        stringRedisTemplate.opsForValue().set(redisKey, "1", CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        log.debug("Device token validated and cached: {}", redisKey);
    }

    /**
     * 处理 URL 编码：Base64 中的 '+' 可能被编码为 %2B 或被错误转成空格。
     */
    private static String normalizeToken(String token) {
        String s = token.trim();
        if (s.indexOf('%') >= 0) {
            s = percentDecode(s);
        }
        return s.replace(' ', '+');
    }

    private static String percentDecode(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int d1 = Character.digit(s.charAt(i + 1), 16);
                int d2 = Character.digit(s.charAt(i + 2), 16);
                if (d1 >= 0 && d2 >= 0) {
                    sb.append((char) ((d1 << 4) | d2));
                    i += 3;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}
