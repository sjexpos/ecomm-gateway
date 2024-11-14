package io.oigres.ecomm.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

@Component
@RefreshScope
@Slf4j
public class LoggingFilter implements GatewayFilter {

    private final MemoryMXBean memoryBean;

    public LoggingFilter() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() != null ? request.getMethod().name() : "";
        String requestUri = request.getURI() != null ? request.getURI().getPath() : "";
        String queryString = request.getURI() != null ? request.getURI().getQuery() : "";
        long start = System.currentTimeMillis();
        long startHeap = memoryBean.getHeapMemoryUsage().getUsed();
        log.info(String.format("BEGIN - %s %s %s", method, requestUri, queryString));
        try {
            return chain.filter(exchange);
        } finally {
            long end = System.currentTimeMillis();
            long endHeap = memoryBean.getHeapMemoryUsage().getUsed();
            log.info(String.format("END - %s %s - status: %s - time: %dms - heap: %s", method, requestUri, exchange.getResponse().getStatusCode(), (end-start), FileUtils.byteCountToDisplaySize(endHeap-startHeap) ));
        }
    }

}
