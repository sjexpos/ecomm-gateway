package io.oigres.ecomm.gateway.services;

import io.oigres.ecomm.gateway.model.BlackInfoBlockedUserMapper;
import io.oigres.ecomm.gateway.model.BlockedUser;
import io.oigres.ecomm.service.limiter.BlackedInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(SpringExtension.class)
public class BlockedUserServiceImplTests {
    @TestConfiguration
    @ComponentScan(basePackageClasses={BlockedUserService.class, BlackInfoBlockedUserMapper.class})
    static class TestConfig {}

    @MockBean(name = "caffeineCacheManager")
    CacheManager caffeineCacheManager;
    @MockBean(name = "redisCacheManager")
    CacheManager redisCacheManager;
    @Autowired
    BlockedUserService blockedUserService;

    @Test
    void test_retrieveBlockedUserFor_default_flow() {
        // given
        String userId = "user14@yopmail.com";

        //when
        BlockedUser actualBlockedUser = this.blockedUserService.retrieveBlockedUserFor(userId);

        // then
        Assertions.assertNull(actualBlockedUser);
    }

    @Test
    void test_processBlackedInfo_non_cached() {
        // given
        String userId = "user14@yopmail.com";
        LocalDateTime from = LocalDateTime.now().minusHours(2);
        LocalDateTime to = LocalDateTime.now();
        Cache redisCache = mock(Cache.class);
        Mockito.doReturn(redisCache).when(this.redisCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);
        Mockito.doReturn(null).when(redisCache).get(eq(userId), eq(BlockedUser.class));
        AtomicReference<String> putUserId = new AtomicReference<>();
        AtomicReference<BlockedUser> putBlockedUser = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            String invokedUserId = invocation.getArgument(0);
            BlockedUser invokedBlockedUser = invocation.getArgument(1);
            putUserId.set(invokedUserId);
            putBlockedUser.set(invokedBlockedUser);
            return null;
        }).when(redisCache).put(any(), any());
        Cache caffeineCache = mock(Cache.class);
        Mockito.doReturn(caffeineCache).when(this.caffeineCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);

        // when
        BlackedInfo blackedInfo = BlackedInfo.builder()
                .userId(userId)
                .from(from)
                .to(to)
                .build();
        this.blockedUserService.processBlackedInfo(blackedInfo);

        // then
        Assertions.assertEquals(userId, putUserId.get());
        Assertions.assertEquals(userId, putBlockedUser.get().getUserId());
        Assertions.assertEquals(from, putBlockedUser.get().getFrom());
        Assertions.assertEquals(to, putBlockedUser.get().getTo());
        Mockito.verify(caffeineCache).evict(eq(userId));
    }

    @Test
    void test_processBlackedInfo_data_cached_but_not_blocked() {
        // given
        String userId = "user14@yopmail.com";
        LocalDateTime from = LocalDateTime.now().minusMinutes(5);
        LocalDateTime to = LocalDateTime.now();
        Cache redisCache = mock(Cache.class);
        Mockito.doReturn(redisCache).when(this.redisCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);
        BlockedUser cachedBlockedUser = BlockedUser.builder()
                .userId(userId)
                .from(LocalDateTime.now().minusHours(10))
                .to(LocalDateTime.now().minusHours(6))
                .build();
        Mockito.doReturn(cachedBlockedUser).when(redisCache).get(eq(userId), eq(BlockedUser.class));
        AtomicReference<String> putUserId = new AtomicReference<>();
        AtomicReference<BlockedUser> putBlockedUser = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            String invokedUserId = invocation.getArgument(0);
            BlockedUser invokedBlockedUser = invocation.getArgument(1);
            putUserId.set(invokedUserId);
            putBlockedUser.set(invokedBlockedUser);
            return null;
        }).when(redisCache).put(any(), any());
        Cache caffeineCache = mock(Cache.class);
        Mockito.doReturn(caffeineCache).when(this.caffeineCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);

        // when
        BlackedInfo blackedInfo = BlackedInfo.builder()
                .userId(userId)
                .from(from)
                .to(to)
                .build();
        this.blockedUserService.processBlackedInfo(blackedInfo);

        // then
        Assertions.assertEquals(userId, putUserId.get());
        Assertions.assertEquals(userId, putBlockedUser.get().getUserId());
        Assertions.assertEquals(from, putBlockedUser.get().getFrom());
        Assertions.assertEquals(to, putBlockedUser.get().getTo());
        Mockito.verify(caffeineCache).evict(eq(userId));
    }

    @Test
    void test_processBlackedInfo_data_cached_and_already_blocked_case1() {
        // given
        String userId = "user14@yopmail.com";
        LocalDateTime cachedFrom = LocalDateTime.now().minusMinutes(5);
        LocalDateTime cachedTo = LocalDateTime.now().plusMinutes(5);
        LocalDateTime from = LocalDateTime.now().minusMinutes(2);
        LocalDateTime to = LocalDateTime.now().plusMinutes(8);
        Cache redisCache = mock(Cache.class);
        Mockito.doReturn(redisCache).when(this.redisCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);
        BlockedUser cachedBlockedUser = BlockedUser.builder()
                .userId(userId)
                .from(cachedFrom)
                .to(cachedTo)
                .build();
        Mockito.doReturn(cachedBlockedUser).when(redisCache).get(eq(userId), eq(BlockedUser.class));
        AtomicReference<String> putUserId = new AtomicReference<>();
        AtomicReference<BlockedUser> putBlockedUser = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            String invokedUserId = invocation.getArgument(0);
            BlockedUser invokedBlockedUser = invocation.getArgument(1);
            putUserId.set(invokedUserId);
            putBlockedUser.set(invokedBlockedUser);
            return null;
        }).when(redisCache).put(any(), any());
        Cache caffeineCache = mock(Cache.class);
        Mockito.doReturn(caffeineCache).when(this.caffeineCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);

        // when
        BlackedInfo blackedInfo = BlackedInfo.builder()
                .userId(userId)
                .from(from)
                .to(to)
                .build();
        this.blockedUserService.processBlackedInfo(blackedInfo);

        // then
        Assertions.assertEquals(userId, putUserId.get());
        Assertions.assertEquals(userId, putBlockedUser.get().getUserId());
        Assertions.assertEquals(cachedFrom, putBlockedUser.get().getFrom());
        Assertions.assertEquals(cachedTo, putBlockedUser.get().getTo());
        Mockito.verify(caffeineCache).evict(eq(userId));
    }

    @Test
    void test_processBlackedInfo_data_cached_and_already_blocked_case2() {
        // given
        String userId = "user14@yopmail.com";
        LocalDateTime cachedFrom = LocalDateTime.now().minusMinutes(3);
        LocalDateTime cachedTo = LocalDateTime.now().plusMinutes(3);
        LocalDateTime from = LocalDateTime.now().minusMinutes(5);
        LocalDateTime to = LocalDateTime.now().plusMinutes(1);
        Cache redisCache = mock(Cache.class);
        Mockito.doReturn(redisCache).when(this.redisCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);
        BlockedUser cachedBlockedUser = BlockedUser.builder()
                .userId(userId)
                .from(cachedFrom)
                .to(cachedTo)
                .build();
        Mockito.doReturn(cachedBlockedUser).when(redisCache).get(eq(userId), eq(BlockedUser.class));
        AtomicReference<String> putUserId = new AtomicReference<>();
        AtomicReference<BlockedUser> putBlockedUser = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            String invokedUserId = invocation.getArgument(0);
            BlockedUser invokedBlockedUser = invocation.getArgument(1);
            putUserId.set(invokedUserId);
            putBlockedUser.set(invokedBlockedUser);
            return null;
        }).when(redisCache).put(any(), any());
        Cache caffeineCache = mock(Cache.class);
        Mockito.doReturn(caffeineCache).when(this.caffeineCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);

        // when
        BlackedInfo blackedInfo = BlackedInfo.builder()
                .userId(userId)
                .from(from)
                .to(to)
                .build();
        this.blockedUserService.processBlackedInfo(blackedInfo);

        // then
        Assertions.assertEquals(userId, putUserId.get());
        Assertions.assertEquals(userId, putBlockedUser.get().getUserId());
        Assertions.assertEquals(from, putBlockedUser.get().getFrom());
        Assertions.assertEquals(to, putBlockedUser.get().getTo());
        Mockito.verify(caffeineCache).evict(eq(userId));
    }

    @Test
    void test_processBlackedInfo_default_flow() {
        // given
        String userId = "user14@yopmail.com";
        LocalDateTime from = LocalDateTime.now().minusHours(2);
        LocalDateTime to = LocalDateTime.now();
        BlackedInfo blackedInfo = BlackedInfo.builder()
                .userId(userId)
                .from(from)
                .to(to)
                .build();

        Cache redisCache = mock(Cache.class);
        Mockito.doReturn(redisCache).when(this.redisCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);
        Mockito.doReturn(null).when(redisCache).get(eq(userId), eq(BlockedUser.class));
        AtomicReference<String> putUserId = new AtomicReference<>();
        AtomicReference<BlockedUser> putBlockedUser = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            String invokedUserId = invocation.getArgument(0);
            BlockedUser invokedBlockedUser = invocation.getArgument(1);
            putUserId.set(invokedUserId);
            putBlockedUser.set(invokedBlockedUser);
            return null;
        }).when(redisCache).put(any(), any());
        Cache caffeineCache = mock(Cache.class);
        Mockito.doReturn(caffeineCache).when(this.caffeineCacheManager).getCache(CacheNames.BLOCKED_USERS_CACHE_NAME);

        // when
        this.blockedUserService.processBlackedInfo(blackedInfo);

        // then
        Assertions.assertEquals(userId, putUserId.get());
        Assertions.assertEquals(userId, putBlockedUser.get().getUserId());
        Assertions.assertEquals(from, putBlockedUser.get().getFrom());
        Assertions.assertEquals(to, putBlockedUser.get().getTo());
        Mockito.verify(caffeineCache).evict(eq(userId));
    }

}
