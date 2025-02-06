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
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.oigres.ecomm.service.users.api.model.ValidateUserResponse;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class to handle JWT tokens.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Component
public class JWTUtil {

  private final SecretKey secret;
  private final String scope;

  public JWTUtil(
      @Value("${ecomm.service.authentication.jwt.secret}") String secretString,
      @Value("${ecomm.service.authentication.scope}") String scope) {
    this.secret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretString));
    this.scope = scope;
  }

  /**
   * Gets claims from a valid JWT token
   *
   * @param token a valid JWT token as string
   * @return a Claims object
   */
  public Claims getAllClaims(String token) {
    return (Claims) Jwts.parser().verifyWith(secret).build().parse(token).getPayload();
  }

  /**
   * Checks if a token has expired or not.
   *
   * @param token a valid JWT token as string
   * @return true if the token have not expired, false otherwise.
   */
  private boolean isTokenExpired(String token) {
    return this.getAllClaims(token).getExpiration().before(new Date());
  }

  /**
   * Checks if a JWT token is well formatted, has a good sign and is not expired.
   *
   * @param token a JWT token
   * @return true if the token has any error, false otherwise.
   */
  public boolean isInvalid(String token) {
    try {
      return this.isTokenExpired(token);
    } catch (MalformedJwtException e) {
      return true;
    }
  }

  /**
   * Creates a JWT token from the ValidateUserResponse object.
   *
   * @param validateUserResponse a response from auth service.
   * @return a valid JWT token
   */
  public String createToken(ValidateUserResponse validateUserResponse) {
    LocalDate exp = LocalDate.now().plusMonths(3);
    return Jwts.builder()
        .issuer("Ecomm")
        .subject(Long.toString(validateUserResponse.getUserId()))
        .claim("name", "user's first name")
        .claim("scope", this.scope)
        .issuedAt(new Date())
        .expiration(Date.from(exp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()))
        .signWith(this.secret)
        .compact();
  }
}
