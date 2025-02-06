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

package io.oigres.ecomm.gateway.util;

import io.jsonwebtoken.Claims;
import io.oigres.ecomm.service.users.api.model.ValidateProfileResponse;
import io.oigres.ecomm.service.users.api.model.ValidateUserResponse;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JWTUtilTests {
  private static final String secret = "Yn2kjibddFAWtnPJ2AFlL8WXmohJMCvigQggaEypa5E=";

  @Test
  void test_create_token() {
    // given
    JWTUtil jwtUtil = new JWTUtil(secret, "admin");
    Long userId = 15L;
    ValidateUserResponse validateUserResponse =
        ValidateUserResponse.builder()
            .userId(userId)
            .profiles(
                List.of(
                    ValidateProfileResponse.builder()
                        .profileId(16L)
                        .profileType("ADMIN")
                        .isEnabled(true)
                        .build()))
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
    ValidateUserResponse validateUserResponse =
        ValidateUserResponse.builder()
            .userId(userId)
            .profiles(
                List.of(
                    ValidateProfileResponse.builder()
                        .profileId(16L)
                        .profileType("ADMIN")
                        .isEnabled(true)
                        .build()))
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
    ValidateUserResponse validateUserResponse =
        ValidateUserResponse.builder()
            .userId(userId)
            .profiles(
                List.of(
                    ValidateProfileResponse.builder()
                        .profileId(16L)
                        .profileType("ADMIN")
                        .isEnabled(true)
                        .build()))
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
