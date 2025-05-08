package com.tokennetwork.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class LoggingAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(LoggingFilterConfig.class)
    public LoggingFilterConfig defaultLoggingFilterConfig() {
        return new LoggingFilterConfig();
    }
}
