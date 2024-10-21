package com.li.bot.config;

import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * @Author: li
 * @CreateTime: 2024-10-19
 */
@Configuration
public class MyBatisConfig {
    @Bean
    public TypeHandlerRegistry typeHandlerRegistry() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        registry.register(UUID.class, new UuidTypeHandler());
        return registry;
    }
}
