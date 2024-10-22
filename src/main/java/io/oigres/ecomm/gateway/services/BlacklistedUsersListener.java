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

    @KafkaListener(topics = "${ecomm.service.limiter.topics.blacklisted-users.name}")
    public void consumeMessage(ConsumerRecord<String, BlackedInfo> record) {
        if (record != null) {
            log.info("Consumed kafka message from '{}'/'{}' in offset '{}' with key '{}'  ", record.topic(), record.partition(), record.offset(), record.key());
        }
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
