package com.hmall.common.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;

/**
 * Feign默认配置,配置Feign日志级别为BASIC
 */
public class DefaultFeignConfig {
    @Bean
    public Logger.Level defaultFeignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
