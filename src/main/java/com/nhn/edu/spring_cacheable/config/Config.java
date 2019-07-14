package com.nhn.edu.spring_cacheable.config;

import com.nhn.edu.spring_cacheable.service.HeavyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    public HeavyService heavyService() {
        return new HeavyService();
    }

}
