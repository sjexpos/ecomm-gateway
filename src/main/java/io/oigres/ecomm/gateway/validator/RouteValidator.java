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

    public Predicate<ServerHttpRequest> isSecured = request -> unprotectedURLs.stream().noneMatch(uri -> request.getURI().getPath().contains(uri));

}
