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

import io.jsonwebtoken.Claims;
import io.oigres.ecomm.service.limiter.ResponseAudit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter class which creates a ResponseAudit object from the http response, and put it in a topic.
 * This message will be received by limiter service.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Component
@Slf4j
public class ResponseAuditFilter implements GatewayFilter {

  private final KafkaTemplate<String, Object> messageKafkaTemplate;

  public ResponseAuditFilter(KafkaTemplate<String, Object> messageKafkaTemplate) {
    this.messageKafkaTemplate = messageKafkaTemplate;
  }

  private Map<String, List<ResponseAudit.HttpCookie>> getCookies(
      MultiValueMap<String, ResponseCookie> cookies) {
    if (cookies == null) {
      return null;
    }
    return cookies.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().stream()
                        .map(
                            c ->
                                ResponseAudit.HttpCookie.builder()
                                    .name(c.getName())
                                    .value(c.getValue())
                                    .maxAge(c.getMaxAge())
                                    .domain(c.getDomain())
                                    .path(c.getPath())
                                    .secure(c.isSecure())
                                    .httpOnly(c.isHttpOnly())
                                    .sameSite(c.getSameSite())
                                    .build())
                        .toList()));
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
        ResponseAudit audit =
            ResponseAudit.builder()
                .id(request.getId())
                .userId(claims.getSubject())
                .headers(response.getHeaders())
                .cookies(getCookies(response.getCookies()))
                .status(response.getStatusCode().value())
                .arrived(LocalDateTime.now())
                .build();
        log.info("Auditing response '{}' for user '{}'", audit.getId(), audit.getUserId());
        this.messageKafkaTemplate.sendDefault(audit.getUserId(), audit);
      }
    }
  }
}
