package io.oigres.ecomm.gateway.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configures kafka access. It creates a template to send message to limiter service for each request/response interaction,
 * and receive message from it when a limit is reached.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public KafkaAdmin admin(@Value("${spring.kafka.bootstrap-servers}") String kafkaBrokers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public KafkaTemplate<String, Object> messageKafkaTemplate(
            ProducerFactory<String, Object> messageProducerFactory,
            KafkaAdmin kafkaAdmin,
            LimiterServiceProperties limiterServiceProperties
    ) {
        kafkaAdmin.createOrModifyTopics(
                new NewTopic(
                        limiterServiceProperties.getTopics().getIncomingRequest().getName(),
                        limiterServiceProperties.getTopics().getIncomingRequest().getPartitions(),
                        limiterServiceProperties.getTopics().getIncomingRequest().getReplicationFactor()
                )
        );
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(messageProducerFactory);
        template.setDefaultTopic(limiterServiceProperties.getTopics().getIncomingRequest().getName());
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            LimiterServiceProperties limiterServiceProperties
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(limiterServiceProperties.getTopics().getBlacklistedUsers().getConcurrency());
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

}
