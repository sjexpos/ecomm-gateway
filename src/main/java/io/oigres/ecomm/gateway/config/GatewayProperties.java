package io.oigres.ecomm.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ecomm.service.gateway")
public class GatewayProperties {

    @NotNull
    @NotBlank
    private String forward;
    @NotNull
    @NotBlank
    private String authServerUri;

}