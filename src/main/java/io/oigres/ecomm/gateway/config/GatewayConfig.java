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

package io.oigres.ecomm.gateway.config;

import io.oigres.ecomm.gateway.filter.*;
import io.oigres.ecomm.gateway.validator.RouteValidator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Configures path forwarding and Open Api 3.0 document transformation to add security scope on each
 * forwarding endpoint.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Configuration
public class GatewayConfig {

  // https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway/cors-configuration.html
  @Bean
  public RouteLocator customRouteLocator(
      RouteLocatorBuilder builder,
      LoggingFilter loggingFilter,
      SignInRewriteFunction signInRewriteFunction,
      AuthFilter authFilter,
      OpenApiEnhacementRewriteFunction openApiEnhacementRewriteFunction,
      RequestAuditFilter requestAuditFilter,
      ResponseAuditFilter responseAuditFilter,
      RequestLimiter requestLimiter,
      GatewayProperties gatewayProperties) {
    return builder
        .routes()
        .route(
            "openapi3_endpoint",
            r ->
                r.path(RouteValidator.OPENAPI_PATH)
                    .filters(
                        f ->
                            f.modifyResponseBody(
                                String.class, String.class, openApiEnhacementRewriteFunction))
                    .uri(gatewayProperties.getForward()))
        .route(
            "signin_endpoint",
            r ->
                r.path(RouteValidator.SIGNIN_PATH)
                    .and()
                    .method(HttpMethod.POST)
                    .filters(
                        f ->
                            f.filter(loggingFilter)
                                .rewritePath(
                                    RouteValidator.SIGNIN_PATH,
                                    RouteValidator.ENDPOINT_FOR_VALIDATE_USERS)
                                .modifyResponseBody(
                                    String.class, String.class, signInRewriteFunction)
                                .filter(responseAuditFilter))
                    .uri(gatewayProperties.getAuthServerUri()))
        .route(
            "secure_endpoints",
            r ->
                r.path(RouteValidator.INTERCEPTED_PATH)
                    .filters(
                        f ->
                            f.filter(loggingFilter)
                                .filter(authFilter)
                                .filter(requestLimiter)
                                .modifyRequestBody(String.class, String.class, requestAuditFilter)
                                .filter(responseAuditFilter))
                    .uri(gatewayProperties.getForward()))
        .build();
  }
}
