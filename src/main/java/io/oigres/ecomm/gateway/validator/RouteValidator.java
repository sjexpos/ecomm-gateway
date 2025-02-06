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

package io.oigres.ecomm.gateway.validator;

import java.util.List;
import java.util.function.Predicate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouteValidator {

  public static final String INTERCEPTED_PATH = "/api/v1/*";

  public static final String OPENAPI_PATH = "/api";

  public static final String SIGNIN_PATH = "/api/v1/auth/signin";

  public static final String ENDPOINT_FOR_VALIDATE_USERS = "/api/v1/users/validate";

  public static final List<String> unprotectedURLs = List.of(SIGNIN_PATH);

  public Predicate<ServerHttpRequest> isSecured =
      request ->
          unprotectedURLs.stream().noneMatch(uri -> request.getURI().getPath().contains(uri));
}
