package com.lehaha.framework.aspectj;

import com.lehaha.common.annotation.DeviceToken;
import com.lehaha.common.exception.ServiceException;
import com.lehaha.common.service.DeviceTokenResolver;
import com.lehaha.common.utils.ServletUtils;
import com.lehaha.common.utils.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class DeviceTokenAspect {
    private static final Logger log = LoggerFactory.getLogger(DeviceTokenAspect.class);

    @Autowired(required = false)
    private DeviceTokenResolver deviceTokenResolver;

    @Before("@annotation(deviceToken)")
    public void validateDeviceToken(JoinPoint joinPoint, DeviceToken deviceToken) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            log.warn("DeviceTokenAspect: HttpServletRequest is null, skip");
            return;
        }

        String token = request.getHeader(deviceToken.header());
        if (StringUtils.isEmpty(token)) {
            if (deviceToken.required()) {
                throw new ServiceException("缺少设备凭证");
            }
            return;
        }

        if (deviceTokenResolver == null) {
            log.warn("DeviceTokenResolver not registered, skip token validation");
            return;
        }

        deviceTokenResolver.validate(token);
    }
}
