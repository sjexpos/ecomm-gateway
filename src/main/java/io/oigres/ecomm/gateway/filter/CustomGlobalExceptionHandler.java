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

import io.oigres.ecomm.gateway.exceptions.UnauthorizedException;
import java.util.Map;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Process exceptions to reach unified error format.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Component
public class CustomGlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

  public CustomGlobalExceptionHandler(
      ErrorAttributes g,
      ApplicationContext applicationContext,
      ServerCodecConfigurer serverCodecConfigurer) {
    super(g, new WebProperties.Resources(), applicationContext);
    super.setMessageWriters(serverCodecConfigurer.getWriters());
    super.setMessageReaders(serverCodecConfigurer.getReaders());
  }

  @Override
  protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
    return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
  }

  private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
    ErrorAttributeOptions options = ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE);
    Map<String, Object> errorPropertiesMap = getErrorAttributes(request, options);
    Throwable throwable = getError(request);
    HttpStatusCode httpStatus = determineHttpStatus(throwable);
    errorPropertiesMap.put("status", httpStatus.value());
    errorPropertiesMap.remove("error");
    return ServerResponse.status(httpStatus)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(errorPropertiesMap));
  }

  private HttpStatusCode determineHttpStatus(Throwable throwable) {
    if (throwable instanceof ResponseStatusException) {
      return ((ResponseStatusException) throwable).getStatusCode();
    } else if (throwable instanceof UnauthorizedException) {
      return HttpStatus.UNAUTHORIZED;
      // } else if (throwable instanceof RateLimitRequestException) {
      //     return HttpStatus.TOO_MANY_REQUESTS;
    } else {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
