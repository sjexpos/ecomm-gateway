package io.oigres.ecomm.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.oigres.ecomm.gateway.exceptions.UnauthorizedException;
import io.oigres.ecomm.gateway.util.JWTUtil;
import io.oigres.ecomm.gateway.validator.RouteValidator;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * This filter intercepts securized request and checks if the jwt token is valid and it is not expired.
 * 
 * @author sjexpos@gmail.com
 */
@Component
@RefreshScope
@Slf4j
public class AuthFilter implements GatewayFilter {

    private final RouteValidator routeValidator;
    private final JWTUtil jwtUtil;
    private final boolean authEnabled;

    public AuthFilter(RouteValidator routeValidator, JWTUtil jwtUtil, @Value("${ecomm.service.authentication.enabled}") boolean authEnabled) {
        this.routeValidator = routeValidator;
        this.jwtUtil = jwtUtil;
        this.authEnabled = authEnabled;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if(!authEnabled) {
            log.info("Authentication is disabled. To enable it, make \"authentication.enabled\" property as true");
            return chain.filter(exchange);
        }
        String token ="";
        ServerHttpRequest request = exchange.getRequest();
        if(routeValidator.isSecured.test(request)) {
            log.info("validating authentication token");
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                throw new UnauthorizedException("Credential is missing");
            }
            String[] authParts = request.getHeaders().get(HttpHeaders.AUTHORIZATION).toString().split(" ");
            if (authParts.length < 2) {
                throw new UnauthorizedException(String.format("%s header is malformed", HttpHeaders.AUTHORIZATION));
            }
            token = authParts[1];
            if(jwtUtil.isInvalid(token)) {
                throw new UnauthorizedException(String.format("%s header is invalid", HttpHeaders.AUTHORIZATION));
            }
            populateRequestWithHeaders(exchange, token);
        }
        return chain.filter(exchange);
    }

    private void populateRequestWithHeaders(ServerWebExchange exchange, String token) {
        Claims claims = jwtUtil.getAllClaims(token);
        exchange.getRequest()
                .mutate()
                .header("id",String.valueOf(claims.get("id")))
                .header("role", String.valueOf(claims.get("role")))
                .build();
    }

}
