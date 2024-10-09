package io.oigres.ecomm.gateway.validator;

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

import java.net.URI;

public class RouteValidatorTests {

    static public class TestServerHttpRequest extends AbstractServerHttpRequest {
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
        ServerHttpRequest serverHttpRequest = new TestServerHttpRequest(
                HttpMethod.GET,
                URI.create(RouteValidator.SIGNIN_PATH),
                ""
        );
        Assertions.assertFalse(routeValidator.isSecured.test(serverHttpRequest));
    }

    @Test
    void test_validate_user_path() {
        RouteValidator routeValidator = new RouteValidator();
        ServerHttpRequest serverHttpRequest = new TestServerHttpRequest(
                HttpMethod.GET,
                URI.create(RouteValidator.ENDPOINT_FOR_VALIDATE_USERS),
                ""
        );
        Assertions.assertTrue(routeValidator.isSecured.test(serverHttpRequest));
    }

    @Test
    void test_other_path() {
        RouteValidator routeValidator = new RouteValidator();
        ServerHttpRequest serverHttpRequest = new TestServerHttpRequest(
                HttpMethod.GET,
                URI.create("/api/v1/products"),
                ""
        );
        Assertions.assertTrue(routeValidator.isSecured.test(serverHttpRequest));
    }

}
