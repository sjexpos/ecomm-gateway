package io.oigres.ecomm.gateway.services;

import io.oigres.ecomm.service.limiter.BlackedInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlacklistedUsersListener {
    private final BlockedUserService blockedUserService;

    @KafkaListener(topics = "${ecomm.service.limiter.topics.blacklisted-users}")
    public void consumeMessage(ConsumerRecord<String, BlackedInfo> record) {
        log.info("Consumed kafka message from blacklisted users topic");
        BlackedInfo blackedInfo = record.value();
        if (blackedInfo != null) {
            try {
                blockedUserService.processBlackedInfo(blackedInfo);
            } catch (Throwable t) {
                log.error("Unexpected error: ", t);
            }
        }
    }

}
