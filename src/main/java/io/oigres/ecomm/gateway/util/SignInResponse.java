package io.oigres.ecomm.gateway.util;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

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
