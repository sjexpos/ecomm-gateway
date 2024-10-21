package io.oigres.ecomm.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure kafka topic when this gateway must send and receive messages.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Data
@ConfigurationProperties(prefix = "ecomm.service.limiter")
public class LimiterServiceProperties {

    @Data
    static public class IncomingRequestTopicProperties {
        @NotNull
        @NotBlank
        private String name;
        private int partitions;
        private short replicationFactor;
    }

    @Data
    static public class BlacklistedUsersTopicProperties {
        @NotNull
        @NotBlank
        private String name;
        private int concurrency;
    }

    @Data
    static public class Topics {
        @NotNull
        @NotBlank
        private IncomingRequestTopicProperties incomingRequest;
        @NotNull
        @NotBlank
        private BlacklistedUsersTopicProperties blacklistedUsers;
    }

    @NotNull
    private Topics topics;

}
