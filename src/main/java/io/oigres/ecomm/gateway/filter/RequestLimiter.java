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
import io.oigres.ecomm.gateway.model.BlockedUser;
import io.oigres.ecomm.gateway.services.BlockedUserService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

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
      if (blockedUser != null && blockedUser.isBlock(LocalDateTime.now())) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
      }
    }
    return chain.filter(exchange);
  }
}
