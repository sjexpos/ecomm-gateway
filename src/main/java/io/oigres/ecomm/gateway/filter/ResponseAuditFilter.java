package io.oigres.ecomm.gateway.filter;

import java.util.Map;
import java.util.HashMap;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        try {
            return chain.filter(exchange);
        } finally {
            Map<String,Object> data = new HashMap<>();
            data.put("id", request.getId());
            data.put("headers", response.getHeaders());
            data.put("cookies", response.getCookies());
            data.put("status", response.getStatusCode().value());
            this.messageKafkaTemplate.send("audit", data);
            }
    }

}
