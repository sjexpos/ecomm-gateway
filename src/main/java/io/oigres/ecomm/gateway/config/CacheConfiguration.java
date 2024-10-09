package io.oigres.ecomm.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.oigres.ecomm.cache.CacheLockFactory;
import io.oigres.ecomm.cache.GzipRedisSerializer;
import io.oigres.ecomm.cache.RedisLockAwareCacheManager;
import io.oigres.ecomm.cache.RedissonCacheLockFactory;
import io.oigres.ecomm.gateway.model.BlockedUser;
import io.oigres.ecomm.gateway.services.CacheNames;
import org.redisson.api.RedissonClient;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures two level cache (caffeine and redis). Also it defines a gzip serializer to send data to redis.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@EnableCaching(mode = AdviceMode.PROXY)
public class CacheConfiguration {

    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCache caffeineCache = new CaffeineCache(CacheNames.BLOCKED_USERS_CACHE_NAME, Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .initialCapacity(1)
                .maximumSize(10000)
                .build());
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Arrays.asList(caffeineCache));
        return manager;
    }

    @Bean
    public CacheLockFactory redisCacheLockFactory(RedissonClient redissonClient) {
        return new RedissonCacheLockFactory(redissonClient);
    }

    @Bean
    public RedissonConnectionFactory redisConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    @Bean
    public CacheManager redisCacheManager(RedissonConnectionFactory connectionFactory, CacheLockFactory cacheLockFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CacheNames.BLOCKED_USERS_CACHE_NAME,
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(new GzipRedisSerializer<>(new Jackson2JsonRedisSerializer<>(objectMapper, BlockedUser.class))))
                        .entryTtl(Duration.ofHours(12)));

        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
        cacheWriter.withStatisticsCollector(CacheStatisticsCollector.create());
        RedisCacheManager cacheManager = new RedisLockAwareCacheManager(
                cacheWriter, RedisCacheConfiguration.defaultCacheConfig(),
                true,
                cacheConfigurations,
                cacheLockFactory);
        cacheManager.setTransactionAware(false);
        return cacheManager;
    }

}
