package io.oigres.ecomm.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.oigres.ecomm.service.users.api.model.ValidateUserResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import javax.crypto.SecretKey;

@Component
public class JWTUtil {

    private final SecretKey secret;
    private final String scope;

    public JWTUtil(
        @Value("${ecomm.service.authentication.jwt.secret}") String secretString,
        @Value("${ecomm.service.authentication.scope}") String scope
        ) {
        this.secret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretString));
        this.scope = scope;
    }

    public Claims getAllClaims(String token) {
        return (Claims)Jwts.parser()
            .verifyWith(secret)
            .build()
            .parse(token)
            .getPayload();
    }

    private boolean isTokenExpired(String token ) {
        return this.getAllClaims(token).getExpiration().before(new Date());
    }

    public boolean isInvalid(String token) {
        try {
            return this.isTokenExpired(token);
        } catch (MalformedJwtException e) {
            return true;
        }
    }

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