package com.hmall.api.config;

import com.hmall.common.utils.UserContext;
import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

/**
 * Feign默认配置,配置Feign日志级别为BASIC
 * 通过RequestInterceptor添加userInfo请求头
 */
public class DefaultFeignConfig {
    @Bean
    public Logger.Level defaultFeignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(feign.RequestTemplate requestTemplate) {
                Long userId = UserContext.getUser();
                if (userId == null) {
                    return;
                }
                String userInfo = userId.toString();
                requestTemplate.header("userInfo", userInfo);
            }
        };
    }
}
