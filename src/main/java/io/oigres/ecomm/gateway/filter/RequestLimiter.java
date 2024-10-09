package io.oigres.ecomm.gateway.filter;

import io.jsonwebtoken.Claims;
import io.oigres.ecomm.gateway.model.BlockedUser;
import io.oigres.ecomm.gateway.services.BlockedUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
@RefreshScope
@Slf4j
@RequiredArgsConstructor
public class RequestLimiter implements GatewayFilter {
    private final BlockedUserService blockedUserService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Claims claims = exchange.getAttribute(AuthFilter.CURRENT_USER_CLAIMS_REQUEST_ATTR);
        if (claims != null) {
            String userId = claims.getSubject();
            BlockedUser blockedUser = this.blockedUserService.retrieveBlockedUserFor(userId);
            if (blockedUser != null && blockedUser.isBlock(LocalDateTime.now()) ) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
        }
        return chain.filter(exchange);
    }

}
