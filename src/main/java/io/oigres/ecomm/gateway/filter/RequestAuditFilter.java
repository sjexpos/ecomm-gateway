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
import io.oigres.ecomm.service.limiter.RequestAudit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Filter class which creates a RequestAudit object from the http request, and put it in a topic.
 * This message will be received by limiter service.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Component
@Slf4j
public class RequestAuditFilter implements RewriteFunction<String, String> {

  private final KafkaTemplate<String, Object> messageKafkaTemplate;

  public RequestAuditFilter(KafkaTemplate<String, Object> messageKafkaTemplate) {
    this.messageKafkaTemplate = messageKafkaTemplate;
  }

  private Map<String, List<RequestAudit.HttpCookie>> getCookies(
      MultiValueMap<String, HttpCookie> cookies) {
    if (cookies == null) {
      return null;
    }
    return cookies.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().stream()
                        .map(c -> new RequestAudit.HttpCookie(c.getName(), c.getValue()))
                        .toList()));
  }

  @Override
  public Publisher<String> apply(ServerWebExchange exchange, String requestBody) {
    Claims claims = exchange.getAttribute(AuthFilter.CURRENT_USER_CLAIMS_REQUEST_ATTR);
    if (claims != null) {
      ServerHttpRequest request = exchange.getRequest();
      RequestAudit audit =
          RequestAudit.builder()
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
      log.info("Auditing request '{}' for user '{}'", audit.getId(), audit.getUserId());
      this.messageKafkaTemplate.sendDefault(audit.getUserId(), audit);
    }
    return requestBody == null ? Mono.empty() : Mono.just(requestBody);
  }
}
