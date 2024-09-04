package io.oigres.ecomm.gateway.filter;

import java.util.Map;
import java.util.HashMap;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class RequestAuditFilter implements RewriteFunction<String,String> {

    private final KafkaTemplate<String,Object> messageKafkaTemplate;

    public RequestAuditFilter(KafkaTemplate<String, Object> messageKafkaTemplate) {
        this.messageKafkaTemplate = messageKafkaTemplate;
    }

    @Override
    public Publisher<String> apply(ServerWebExchange exchange, String requestBody) {
        ServerHttpRequest request = exchange.getRequest();
        Map<String,Object> data = new HashMap<>();
        data.put("id", request.getId());
        data.put("remote_addr", request.getRemoteAddress());
        data.put("method", request.getMethod().name());
        data.put("path", request.getPath().value());
        data.put("query", request.getQueryParams());
        data.put("headers", request.getHeaders());
        data.put("cookies", request.getCookies());
        data.put("body", request.getBody());
        this.messageKafkaTemplate.send("audit", data);
        return requestBody == null ? Mono.empty() : Mono.just(requestBody);
    }

}
