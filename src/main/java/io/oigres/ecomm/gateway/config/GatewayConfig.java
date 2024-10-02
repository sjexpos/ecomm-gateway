package io.oigres.ecomm.gateway.config;

import io.oigres.ecomm.gateway.filter.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import io.oigres.ecomm.gateway.validator.RouteValidator;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
public class GatewayConfig {

    // https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/cors-configuration.html
    @Bean
    public RouteLocator customRouteLocator(
            RouteLocatorBuilder builder,
            SignInRewriteFunction signInRewriteFunction,
            AuthFilter authFilter,
            OpenApiEnhacementRewriteFunction openApiEnhacementRewriteFunction,
            RequestAuditFilter requestAuditFilter,
            ResponseAuditFilter responseAuditFilter,
            RequestLimiter requestLimiter,
            GatewayProperties gatewayProperties
    ) {
        return builder.routes()
                .route("openapi3_endpoint", r -> r.path(RouteValidator.OPENAPI_PATH)
                        .filters(
                            f -> f.modifyResponseBody(String.class, String.class, openApiEnhacementRewriteFunction)
                        )
                        .uri(gatewayProperties.getForward()))
                .route("signin_endpoint", r -> r.path(RouteValidator.SIGNIN_PATH)
                        .and().method(HttpMethod.POST)
                        .filters(f -> f
                            .rewritePath(RouteValidator.SIGNIN_PATH, RouteValidator.ENDPOINT_FOR_VALIDATE_USERS)
                            .modifyResponseBody(String.class, String.class, signInRewriteFunction)
                            .filter(responseAuditFilter)
                        )
                        .uri(gatewayProperties.getAuthServerUri()))
                .route("secure_endpoints", r -> r.path(RouteValidator.INTERCEPTED_PATH)
                        .filters(f -> f.filter(authFilter)
                            .filter(requestLimiter)
                            .modifyRequestBody(String.class, String.class, requestAuditFilter)
                            .filter(responseAuditFilter)
                        )
                        .uri(gatewayProperties.getForward()))
                .build();
    }

}
