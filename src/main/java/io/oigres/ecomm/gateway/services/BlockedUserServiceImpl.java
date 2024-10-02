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
        BlockedUser blockedUser = redisCache.get(info.getUserId(), BlockedUser.class);
        if  (blockedUser == null || !blockedUser.isBlock(LocalDateTime.now())) {
            blockedUser = this.mapper.from(info);
            updateCachedBlockedUser(blockedUser);
        } else if (!blockedUser.isBlock(info.getFrom()) || !blockedUser.isBlock(info.getTo())) {
            blockedUser = BlockedUser.builder()
                    .userId(info.getUserId())
                    .from(Stream.of(info.getFrom(), blockedUser.getFrom()).min(LocalDateTime::compareTo).get())
                    .to(Stream.of(info.getTo(), blockedUser.getTo()).min(LocalDateTime::compareTo).get())
                    .build();
            updateCachedBlockedUser(blockedUser);
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
