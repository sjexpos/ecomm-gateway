package io.oigres.ecomm.gateway.services;

import io.oigres.ecomm.service.limiter.BlackedInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

@ExtendWith(SpringExtension.class)
public class BlacklistedUsersListenerTests {

    @TestConfiguration
    @ComponentScan
    static class TestConfig {}

    @MockBean BlockedUserService blockedUserService;
    @Autowired BlacklistedUsersListener blacklistedUsersListener;

    @Test
    void test_default_flow() {
        // given
        String userId = "user14@yopmail.com";
        LocalDateTime from = LocalDateTime.now().minusHours(2);
        LocalDateTime to = LocalDateTime.now();
        BlackedInfo blackedInfo = BlackedInfo.builder()
                .userId(userId)
                .from(from)
                .to(to)
                .build();
        ConsumerRecord<String, BlackedInfo> record = new ConsumerRecord<>("my_topic", 1, 15, userId, blackedInfo);
        willDoNothing().given(this.blockedUserService).processBlackedInfo(any());

        // when
        blacklistedUsersListener.consumeMessage(record);

        // then
        ArgumentCaptor<BlackedInfo> blackedInfoCaptor = ArgumentCaptor.forClass(BlackedInfo.class);
        then(this.blockedUserService).should().processBlackedInfo(blackedInfoCaptor.capture());
        Assertions.assertNotNull(blackedInfoCaptor.getValue());
        Assertions.assertEquals(userId, blackedInfoCaptor.getValue().getUserId());
        Assertions.assertEquals(from, blackedInfoCaptor.getValue().getFrom());
        Assertions.assertEquals(to, blackedInfoCaptor.getValue().getTo());
    }

    @Test
    void test_null_blacked_info_flow() {
        // given
        String userId = "user14@yopmail.com";
        ConsumerRecord<String, BlackedInfo> record = new ConsumerRecord<>("my_topic", 1, 15, userId, null);
        willDoNothing().given(this.blockedUserService).processBlackedInfo(any());

        // when
        blacklistedUsersListener.consumeMessage(record);

        // then
        then(this.blockedUserService).shouldHaveNoInteractions();
    }

    @Test
    void test_not_catch_exception_flow() {
        //given
        String userId = "user14@yopmail.com";
        LocalDateTime from = LocalDateTime.now().minusHours(2);
        LocalDateTime to = LocalDateTime.now();
        BlackedInfo blackedInfo = BlackedInfo.builder()
                .userId(userId)
                .from(from)
                .to(to)
                .build();
        ConsumerRecord<String, BlackedInfo> record = new ConsumerRecord<>("my_topic", 1, 15, userId, blackedInfo);
        willThrow(new RuntimeException()).given(this.blockedUserService).processBlackedInfo(any());

        // when
        blacklistedUsersListener.consumeMessage(record);

        // then
        ArgumentCaptor<BlackedInfo> blackedInfoCaptor = ArgumentCaptor.forClass(BlackedInfo.class);
        then(this.blockedUserService).should().processBlackedInfo(blackedInfoCaptor.capture());
        Assertions.assertNotNull(blackedInfoCaptor.getValue());
        Assertions.assertEquals(userId, blackedInfoCaptor.getValue().getUserId());
        Assertions.assertEquals(from, blackedInfoCaptor.getValue().getFrom());
        Assertions.assertEquals(to, blackedInfoCaptor.getValue().getTo());
    }

}
