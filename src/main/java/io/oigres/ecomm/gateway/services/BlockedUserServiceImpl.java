package io.oigres.ecomm.gateway.services;

import io.oigres.ecomm.cache.annotations.CacheLock;
import io.oigres.ecomm.gateway.model.BlackInfoBlockedUserMapper;
import io.oigres.ecomm.gateway.model.BlockedUser;
import io.oigres.ecomm.service.limiter.BlackedInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.stream.Stream;

/**
 * Service class to store and retrieve blocked users from cache.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Slf4j
@Service
public class BlockedUserServiceImpl implements BlockedUserService {
    private final CacheManager caffeineCacheManager;
    private final CacheManager redisCacheManager;
    private final BlackInfoBlockedUserMapper mapper;

    public BlockedUserServiceImpl(
            @Qualifier("caffeineCacheManager") CacheManager caffeineCacheManager,
            @Qualifier("redisCacheManager") CacheManager redisCacheManager,
            BlackInfoBlockedUserMapper mapper
    ) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.mapper = mapper;
    }

    private void updateCachedBlockedUser(BlockedUser blockedUser) {
        log.info("User '{}' will be blocked from {} to {}", blockedUser.getUserId(), blockedUser.getFrom(), blockedUser.getTo());
        Cache redisCache = this.redisCacheManager.getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);
        redisCache.put(blockedUser.getUserId(), blockedUser);
        Cache caffeineCache = this.caffeineCacheManager.getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);
        caffeineCache.evict(blockedUser.getUserId()); // refresh first cache level
    }

    @CacheLock
    public void processBlackedInfo(BlackedInfo info) {
        if (!StringUtils.hasText(info.getUserId())) {
            return;
        }
        Cache redisCache = this.redisCacheManager.getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);
        BlockedUser cachedBlockedUser = redisCache.get(info.getUserId(), BlockedUser.class);
        if  (cachedBlockedUser == null || !cachedBlockedUser.isBlock(LocalDateTime.now())) {
            cachedBlockedUser = this.mapper.from(info);
            updateCachedBlockedUser(cachedBlockedUser);
        } else if (!cachedBlockedUser.isBlock(info.getFrom()) || !cachedBlockedUser.isBlock(info.getTo())) {
            cachedBlockedUser = BlockedUser.builder()
                    .userId(info.getUserId())
                    .from(Stream.of(info.getFrom(), cachedBlockedUser.getFrom()).min(LocalDateTime::compareTo).get())
                    .to(Stream.of(info.getTo(), cachedBlockedUser.getTo()).min(LocalDateTime::compareTo).get())
                    .build();
            updateCachedBlockedUser(cachedBlockedUser);
        }
    }

    @Caching(cacheable = {
            @Cacheable(key = "#userId", cacheNames = CacheNames.BLOCKED_USERS_CACHE_NAME, cacheManager = "caffeineCacheManager"),
            @Cacheable(key = "#userId", cacheNames = CacheNames.BLOCKED_USERS_CACHE_NAME, cacheManager = "redisCacheManager")
    })
    public BlockedUser retrieveBlockedUserFor(String userId) {
        return null;
    }

}
