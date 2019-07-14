package com.nhn.edu.spring_cacheable.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

@EnableCaching(proxyTargetClass = true)
@Configuration
public class CacheConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    public RedisServer redisServer() {
        // cf.) https://github.com/kstyrc/embedded-redis/issues/51
        return RedisServer.builder()
                          .setting("maxmemory 128M")
                          .build();
    }

}
