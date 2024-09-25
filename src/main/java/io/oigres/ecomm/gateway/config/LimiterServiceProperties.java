package io.oigres.ecomm.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ecomm.service.limiter")
public class LimiterServiceProperties {

    @Data
    static public class Topics {
        @NotNull
        @NotBlank
        private String incomingRequest;
    }

    @NotNull
    private Topics topics;

}
