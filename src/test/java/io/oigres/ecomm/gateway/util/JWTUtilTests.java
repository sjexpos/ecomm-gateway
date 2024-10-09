package io.oigres.ecomm.gateway.util;

import io.jsonwebtoken.Claims;
import io.oigres.ecomm.service.users.api.model.ValidateProfileResponse;
import io.oigres.ecomm.service.users.api.model.ValidateUserResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class JWTUtilTests {
    private static final String secret = "Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=";

    @Test
    void test_create_token() {
        // given
        JWTUtil jwtUtil = new JWTUtil(secret, "admin");
        Long userId = 15L;
        ValidateUserResponse validateUserResponse = ValidateUserResponse.builder()
                .userId(userId)
                .profiles(List.of(
                        ValidateProfileResponse.builder()
                                .profileId(16L)
                                .profileType("ADMIN")
                                .isEnabled(true)
                                .build()
                ))
                .build();

        // when
        String token = jwtUtil.createToken(validateUserResponse);

        // then
        Assertions.assertNotNull(token);
        Assertions.assertTrue(token.length() > 10);
    }

    @Test
    void test_get_claims() {
        // given
        JWTUtil jwtUtil = new JWTUtil(secret, "admin");
        Long userId = 15L;
        ValidateUserResponse validateUserResponse = ValidateUserResponse.builder()
                .userId(userId)
                .profiles(List.of(
                        ValidateProfileResponse.builder()
                                .profileId(16L)
                                .profileType("ADMIN")
                                .isEnabled(true)
                                .build()
                ))
                .build();
        String token = jwtUtil.createToken(validateUserResponse);

        // when
        Claims claims = jwtUtil.getAllClaims(token);

        // then
        Assertions.assertNotNull(claims);
        Assertions.assertEquals("Ecomm", claims.getIssuer());
        Assertions.assertEquals("15", claims.getSubject());
        Assertions.assertEquals("admin", claims.get("scope"));
        Assertions.assertNotNull(claims.getIssuedAt());
        Assertions.assertNotNull(claims.getExpiration());
    }

    @Test
    void test_is_valid() {
        // given
        JWTUtil jwtUtil = new JWTUtil(secret, "admin");
        Long userId = 15L;
        ValidateUserResponse validateUserResponse = ValidateUserResponse.builder()
                .userId(userId)
                .profiles(List.of(
                        ValidateProfileResponse.builder()
                                .profileId(16L)
                                .profileType("ADMIN")
                                .isEnabled(true)
                                .build()
                ))
                .build();
        String token = jwtUtil.createToken(validateUserResponse);

        // when
        boolean isValid = jwtUtil.isInvalid(token);

        // then
        Assertions.assertFalse(isValid);
    }

    @Test
    void test_is_not_valid() {
        // given
        JWTUtil jwtUtil = new JWTUtil(secret, "admin");
        String token = "aflkjnbqworhgiqwrtgu[oipjsadfvb";

        // when
        boolean isValid = jwtUtil.isInvalid(token);

        // then
        Assertions.assertTrue(isValid);
    }

}
