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
import io.oigres.ecomm.gateway.config.AuthenticationProperties;
import io.oigres.ecomm.gateway.exceptions.UnauthorizedException;
import io.oigres.ecomm.gateway.util.JWTUtil;
import io.oigres.ecomm.gateway.validator.RouteValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * This filter intercepts secured request and checks if the jwt token is valid, and it is not
 * expired.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Component
@RefreshScope
@Slf4j
@RequiredArgsConstructor
public class AuthFilter implements GatewayFilter {
  public static final String CURRENT_USER_CLAIMS_REQUEST_ATTR = "CURRENT_USER_CLAIMS_REQUEST_ATTR";

  private final RouteValidator routeValidator;
  private final JWTUtil jwtUtil;
  private final AuthenticationProperties authenticationProperties;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!this.authenticationProperties.isEnabled()) {
      log.warn(
          "Authentication is disabled. To enable it, make \"authentication.enabled\" property as"
              + " true");
      return chain.filter(exchange);
    }
    String token = "";
    ServerHttpRequest request = exchange.getRequest();
    if (routeValidator.isSecured.test(request)) {
      log.debug("validating authentication token");
      if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        log.info("Header {} is missing", HttpHeaders.AUTHORIZATION);
        throw new UnauthorizedException("Credential is missing");
      }
      String[] authParts =
          request.getHeaders().get(HttpHeaders.AUTHORIZATION).stream().findFirst().get().split(" ");
      if (authParts.length < 2) {
        log.info("Header {} does not have 2 parts", HttpHeaders.AUTHORIZATION);
        throw new UnauthorizedException(
            String.format("%s header is malformed", HttpHeaders.AUTHORIZATION));
      }
      if (!"bearer".equalsIgnoreCase(authParts[0])) {
        log.info("Header {} does not have the prefix 'Bearer'", HttpHeaders.AUTHORIZATION);
        throw new UnauthorizedException(
            String.format("%s header is malformed", HttpHeaders.AUTHORIZATION));
      }
      token = authParts[1];
      if (jwtUtil.isInvalid(token)) {
        throw new UnauthorizedException(
            String.format("%s header is invalid", HttpHeaders.AUTHORIZATION));
      }
      populateRequestWithHeaders(exchange, token);
    }
    return chain.filter(exchange);
  }

  /**
   * Sticks user information to the current request/response interaction.
   *
   * @param exchange ServerWebExchange object
   * @param token JWT token which was gotten from Authorization header
   */
  private void populateRequestWithHeaders(ServerWebExchange exchange, String token) {
    Claims claims = jwtUtil.getAllClaims(token);
    exchange.getAttributes().put(CURRENT_USER_CLAIMS_REQUEST_ATTR, claims);
  }
}
