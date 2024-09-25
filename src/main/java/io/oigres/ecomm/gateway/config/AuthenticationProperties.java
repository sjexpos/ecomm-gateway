package io.oigres.ecomm.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ecomm.service.authentication")
public class AuthenticationProperties {

    @Data
    static public class Jwt {
        @NotNull
        @NotBlank
        private String secret;
    }

    @NotNull
    @NotBlank
    private String scope;
    private boolean enabled;
    @NotNull
    private Jwt jwt;
}
