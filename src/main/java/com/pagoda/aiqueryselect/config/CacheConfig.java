package com.pagoda.aiqueryselect.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;


/**
 * date           : 2026-02-05
 * description    :
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2026-02-05        user       최초 생성
 */
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.
            setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS).maximumSize(50));
        return manager;
    }

}
