package io.oigres.ecomm.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure distributed tracing.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Data
@ConfigurationProperties(prefix = "ecomm.service.tracing")
public class TracingProperties {
    @NotNull
    @NotBlank
    private String url;
}
