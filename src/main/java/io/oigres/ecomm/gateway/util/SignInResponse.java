package io.oigres.ecomm.gateway.util;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO object which is respond then the user sign-in.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Builder
@Getter
public class SignInResponse {
    
    @JsonProperty("user_id")
    private String userid;
    @JsonProperty("name")
    private String name;
    @JsonProperty("jwt")
    private String token;
}
