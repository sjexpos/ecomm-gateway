package io.oigres.ecomm.gateway.filter;

import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.oigres.ecomm.gateway.util.JWTUtil;
import io.oigres.ecomm.gateway.util.SignInResponse;
import io.oigres.ecomm.gateway.validator.RouteValidator;
import io.oigres.ecomm.service.users.api.model.ValidateUserResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * This filter intercepts the request from authentication service and creates a JWT token for the signed-in user.
 * 
 * @author sjexpos@gmail.com
 */
@Component
@Slf4j
public class SignInRewriteFunction implements RewriteFunction<String,String> {
    private static final String ERROR_RESPONSE_PATH_FIELD_NAME = "path";
    private static final String ERROR_RESPONSE_EXCEPTION_FIELD_NAME = "exception";

    private final ObjectMapper mapper;
    private final JWTUtil jwtUtil;

    public SignInRewriteFunction(JWTUtil jwtUtil) {
        this.mapper = new ObjectMapper();
        this.mapper.setSerializationInclusion(Include.NON_NULL);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Publisher<String> apply(ServerWebExchange t, String responseBody) {
        if (HttpStatus.OK.equals(t.getResponse().getStatusCode())) {
            try {
                ValidateUserResponse validateUserResponse = this.mapper.readValue(responseBody, ValidateUserResponse.class);
                String token = this.jwtUtil.createToken(validateUserResponse);
                SignInResponse signInResponse = SignInResponse.builder()
                    .userid(Long.toString(validateUserResponse.getUserId()))
                    .name("")
                    .token(token)
                    .build();
                responseBody = this.mapper.writeValueAsString(signInResponse);
            } catch (JsonProcessingException e) {
                log.error("", e);
            }
        } else {
            try {
                ObjectNode error = (ObjectNode)this.mapper.readTree(responseBody);
                error.put(ERROR_RESPONSE_PATH_FIELD_NAME, RouteValidator.SIGNIN_PATH);
                if (error.has(ERROR_RESPONSE_EXCEPTION_FIELD_NAME)) {
                    String exception = error.get(ERROR_RESPONSE_EXCEPTION_FIELD_NAME).asText();
                    exception = exception.substring(exception.lastIndexOf(".")+1);
                    error.put(ERROR_RESPONSE_EXCEPTION_FIELD_NAME, exception);
                }
                responseBody = this.mapper.writeValueAsString(error);
            } catch (JsonProcessingException e) {
                log.error("", e);
            }
        }
        return Mono.just(responseBody);
    }

}
