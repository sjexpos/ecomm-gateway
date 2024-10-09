package io.oigres.ecomm.gateway.filter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jsonwebtoken.Claims;
import io.oigres.ecomm.service.limiter.RequestAudit;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Filter class which creates a RequestAudit object from the http request, and put it in a topic.
 * This message will be received by limiter service.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Component
@Slf4j
public class RequestAuditFilter implements RewriteFunction<String,String> {

    private final KafkaTemplate<String,Object> messageKafkaTemplate;

    public RequestAuditFilter(KafkaTemplate<String, Object> messageKafkaTemplate) {
        this.messageKafkaTemplate = messageKafkaTemplate;
    }

    private Map<String, List<RequestAudit.HttpCookie>> getCookies(MultiValueMap<String, HttpCookie> cookies) {
        if (cookies == null) {
            return null;
        }
        return cookies.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream().map(c -> new RequestAudit.HttpCookie(c.getName(), c.getValue())).toList()
                        )
                );
    }

    @Override
    public Publisher<String> apply(ServerWebExchange exchange, String requestBody) {
        Claims claims = exchange.getAttribute(AuthFilter.CURRENT_USER_CLAIMS_REQUEST_ATTR);
        if (claims != null) {
            ServerHttpRequest request = exchange.getRequest();
            RequestAudit audit = RequestAudit.builder()
                    .id(request.getId())
                    .userId(claims.getSubject())
                    .remoteAddr(request.getRemoteAddress().toString())
                    .method(request.getMethod().name())
                    .path(request.getPath().value())
                    .query(request.getQueryParams())
                    .headers(request.getHeaders())
                    .cookies(getCookies(request.getCookies()))
                    .body(requestBody)
                    .arrived(LocalDateTime.now())
                    .build();
            this.messageKafkaTemplate.sendDefault(audit.getUserId(), audit);
        }
        return requestBody == null ? Mono.empty() : Mono.just(requestBody);
    }

}
