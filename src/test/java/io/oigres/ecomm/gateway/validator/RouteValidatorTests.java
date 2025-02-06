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

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

public class RouteValidatorTests {

  public static class TestServerHttpRequest extends AbstractServerHttpRequest {
    public TestServerHttpRequest(HttpMethod method, URI uri, String contextPath) {
      super(method, uri, contextPath, new HttpHeaders());
    }

    @Override
    protected MultiValueMap<String, HttpCookie> initCookies() {
      return null;
    }

    @Override
    protected SslInfo initSslInfo() {
      return null;
    }

    @Override
    public <T> T getNativeRequest() {
      return null;
    }

    @Override
    public Flux<DataBuffer> getBody() {
      return null;
    }
  }

  @Test
  void test_singin_path() {
    RouteValidator routeValidator = new RouteValidator();
    ServerHttpRequest serverHttpRequest =
        new TestServerHttpRequest(HttpMethod.GET, URI.create(RouteValidator.SIGNIN_PATH), "");
    Assertions.assertFalse(routeValidator.isSecured.test(serverHttpRequest));
  }

  @Test
  void test_validate_user_path() {
    RouteValidator routeValidator = new RouteValidator();
    ServerHttpRequest serverHttpRequest =
        new TestServerHttpRequest(
            HttpMethod.GET, URI.create(RouteValidator.ENDPOINT_FOR_VALIDATE_USERS), "");
    Assertions.assertTrue(routeValidator.isSecured.test(serverHttpRequest));
  }

  @Test
  void test_other_path() {
    RouteValidator routeValidator = new RouteValidator();
    ServerHttpRequest serverHttpRequest =
        new TestServerHttpRequest(HttpMethod.GET, URI.create("/api/v1/products"), "");
    Assertions.assertTrue(routeValidator.isSecured.test(serverHttpRequest));
  }
}
