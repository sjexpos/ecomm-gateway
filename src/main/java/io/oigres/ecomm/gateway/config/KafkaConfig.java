/**********
 This project is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the
 Free Software Foundation; either version 3.0 of the License, or (at your
 option) any later version. (See <https://www.gnu.org/licenses/gpl-3.0.html>.)

 This project is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 more details.

 You should have received a copy of the GNU General Public License
 along with this project; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 **********/
// Copyright (c) 2024-2025 Sergio Exposito.  All rights reserved.              

package io.oigres.ecomm.gateway.config;

import io.micrometer.common.KeyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.micrometer.KafkaRecordSenderContext;
import org.springframework.kafka.support.micrometer.KafkaTemplateObservationConvention;

/**
 * Configures kafka access. It creates a template to send message to limiter service for each
 * request/response interaction, and receive message from it when a limit is reached.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Configuration
@EnableKafka
public class KafkaConfig {

  //    @Bean
  //    public KafkaAdmin admin(@Value("${spring.kafka.bootstrap-servers}") String kafkaBrokers) {
  //        Map<String, Object> configs = new HashMap<>();
  //        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
  //        return new KafkaAdmin(configs);
  //    }

  @Bean
  public KafkaTemplate<String, Object> messageKafkaTemplate(
      ProducerFactory<String, Object> messageProducerFactory,
      //            KafkaAdmin kafkaAdmin,
      LimiterServiceProperties limiterServiceProperties) {
    // This topic creation from code requires more permission on Kafka, so it is commented
    //        kafkaAdmin.createOrModifyTopics(
    //                new NewTopic(
    //                        limiterServiceProperties.getTopics().getIncomingRequest().getName(),
    //
    // limiterServiceProperties.getTopics().getIncomingRequest().getPartitions(),
    //
    // limiterServiceProperties.getTopics().getIncomingRequest().getReplicationFactor()
    //                )
    //        );
    KafkaTemplate<String, Object> template = new KafkaTemplate<>(messageProducerFactory);
    template.setDefaultTopic(limiterServiceProperties.getTopics().getIncomingRequest().getName());
    template.setObservationEnabled(true);
    template.setObservationConvention(
        new KafkaTemplateObservationConvention() {
          @Override
          public KeyValues getLowCardinalityKeyValues(KafkaRecordSenderContext context) {
            return KeyValues.of(
                "custom_tag_topic",
                context.getDestination(),
                "custom_tag_record_id",
                String.valueOf(context.getRecord().key()));
          }
        });
    return template;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory,
      LimiterServiceProperties limiterServiceProperties) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setConcurrency(
        limiterServiceProperties.getTopics().getBlacklistedUsers().getConcurrency());
    factory.getContainerProperties().setObservationEnabled(true);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    return factory;
  }
}
