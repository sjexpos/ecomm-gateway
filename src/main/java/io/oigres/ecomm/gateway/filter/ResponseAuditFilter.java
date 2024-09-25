package io.oigres.ecomm.gateway.filter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import io.jsonwebtoken.Claims;
import io.oigres.ecomm.service.limiter.ResponseAudit;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ResponseAuditFilter implements GatewayFilter {

    private final KafkaTemplate<String,Object> messageKafkaTemplate;

    public ResponseAuditFilter(KafkaTemplate<String, Object> messageKafkaTemplate) {
        this.messageKafkaTemplate = messageKafkaTemplate;
    }

    private Map<String, List<ResponseAudit.HttpCookie>> getCookies(MultiValueMap<String, ResponseCookie> cookies) {
        if (cookies == null) {
            return null;
        }
        return cookies.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream().map(c ->
                                        ResponseAudit.HttpCookie.builder()
                                                .name(c.getName())
                                                .value(c.getValue())
                                                .maxAge(c.getMaxAge())
                                                .domain(c.getDomain())
                                                .path(c.getPath())
                                                .secure(c.isSecure())
                                                .httpOnly(c.isHttpOnly())
                                                .sameSite(c.getSameSite())
                                                .build()
                                ).toList()
                        )
                );
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        try {
            return chain.filter(exchange);
        } finally {
            Claims claims = exchange.getAttribute(AuthFilter.CURRENT_USER_CLAIMS_REQUEST_ATTR);
            if (claims != null) {
                ResponseAudit audit = ResponseAudit.builder()
                        .id(request.getId())
                        .userId(claims.getSubject())
                        .headers(response.getHeaders())
                        .cookies(getCookies(response.getCookies()))
                        .status(response.getStatusCode().value())
                        .arrived(LocalDateTime.now())
                        .build();
                this.messageKafkaTemplate.sendDefault(audit.getUserId(), audit);
            }
        }
    }

}
