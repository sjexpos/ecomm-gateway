/**********
 This project is free software; you can redistribute it and/or modify it under
 the terms of the GNU General Public License as published by the
 Free Software Foundation; either version 3.0 of the License, or (at your
 option) any later version. (See <https://www.gnu.org/licenses/gpl-3.0.html>.)

 This project is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 more details.

 You should have received a copy of the GNU General Public License
 along with this project; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 **********/
// Copyright (c) 2024-2025 Sergio Exposito.  All rights reserved.              

package io.oigres.ecomm.gateway.filter;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
      log.info(
          String.format(
              "END - %s %s - status: %s - time: %dms - heap: %s",
              method,
              requestUri,
              exchange.getResponse().getStatusCode(),
              (end - start),
              FileUtils.byteCountToDisplaySize(endHeap - startHeap)));
    }
  }
}
