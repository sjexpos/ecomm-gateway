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

package io.oigres.ecomm.gateway.filter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.oigres.ecomm.gateway.GatewayApplication;
import io.oigres.ecomm.gateway.model.BlockedUser;
import io.oigres.ecomm.gateway.services.BlockedUserService;
import io.oigres.ecomm.gateway.util.JWTUtil;
import io.oigres.ecomm.gateway.util.SignInResponse;
import io.oigres.ecomm.service.limiter.RequestAudit;
import io.oigres.ecomm.service.limiter.ResponseAudit;
import io.oigres.ecomm.service.users.api.model.ValidateProfileResponse;
import io.oigres.ecomm.service.users.api.model.ValidateUserRequest;
import io.oigres.ecomm.service.users.api.model.ValidateUserResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(
    classes = GatewayApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
@TestPropertySource(
    locations = "classpath:application-integrationtest.properties",
    properties =
        """
server.port=10000
ecomm.service.gateway.forward=http://localhost:20001
ecomm.service.gateway.auth-server-uri=http://localhost:20002
                """)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
public class FiltersITests {

  @RegisterExtension
  static WireMockExtension FORWARD_SERVICE =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().port(20001))
          .build();

  @RegisterExtension
  static WireMockExtension AUTH_SERVICE =
      WireMockExtension.newInstance()
          .options(WireMockConfiguration.wireMockConfig().port(20002))
          .build();

  @Autowired JWTUtil jwtUtil;
  @MockBean KafkaTemplate<String, Object> messageKafkaTemplate;
  @MockBean BlockedUserService blockedUserService;

  private RestTemplateBuilder builder = new RestTemplateBuilder().rootUri("http://localhost:10000");
  private RestTemplate restTemplate;

  @BeforeEach
  void setup() {
    this.restTemplate = builder.build();
  }

  @Test
  void test_secured_endpoint() throws Exception {
    Assertions.assertThrowsExactly(
        HttpClientErrorException.Unauthorized.class,
        () -> restTemplate.getForEntity("/api/v1/categories", String.class));
  }

  @Test
  void test_default_flow() {
    // given
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

    String body = "{ \"call\":\"ok\"}";
    FORWARD_SERVICE.stubFor(
        get(urlEqualTo("/api/v1/categories"))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                    .withBody(body)));

    // when
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<?> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response =
        restTemplate.exchange("/api/v1/categories", HttpMethod.GET, entity, String.class);

    // then
    Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    Assertions.assertEquals(body, response.getBody());
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
    then(this.messageKafkaTemplate)
        .should(times(2))
        .sendDefault(keyCaptor.capture(), payloadCaptor.capture());
    Assertions.assertNotNull(keyCaptor.getAllValues());
    Assertions.assertEquals(2, keyCaptor.getAllValues().size());
    Assertions.assertEquals("15", keyCaptor.getAllValues().get(0));
    Assertions.assertEquals("15", keyCaptor.getAllValues().get(1));
    Assertions.assertNotNull(payloadCaptor.getAllValues());
    Assertions.assertEquals(2, payloadCaptor.getAllValues().size());
    Assertions.assertInstanceOf(RequestAudit.class, payloadCaptor.getAllValues().get(0));
    RequestAudit requestAudit = (RequestAudit) payloadCaptor.getAllValues().get(0);
    Assertions.assertNotNull(requestAudit.getId());
    Assertions.assertEquals("15", requestAudit.getUserId());
    Assertions.assertNotNull(requestAudit.getRemoteAddr());
    Assertions.assertEquals("GET", requestAudit.getMethod());
    Assertions.assertEquals("/api/v1/categories", requestAudit.getPath());
    Assertions.assertNotNull(requestAudit.getQuery());
    Assertions.assertNotNull(requestAudit.getHeaders());
    Assertions.assertNotNull(requestAudit.getCookies());
    Assertions.assertNull(requestAudit.getBody());
    Assertions.assertNotNull(requestAudit.getArrived());
    Assertions.assertInstanceOf(ResponseAudit.class, payloadCaptor.getAllValues().get(1));
    ResponseAudit responseAudit = (ResponseAudit) payloadCaptor.getAllValues().get(1);
    Assertions.assertEquals(requestAudit.getId(), responseAudit.getId());
    Assertions.assertEquals("15", responseAudit.getUserId());
    Assertions.assertNotNull(responseAudit.getHeaders());
    Assertions.assertNotNull(responseAudit.getCookies());
    Assertions.assertEquals(200, responseAudit.getStatus());
    Assertions.assertNotNull(responseAudit.getArrived());
  }

  @Test
  void test_rate_limit() {
    // given
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
    BlockedUser blockedUser =
        BlockedUser.builder()
            .userId("15")
            .from(LocalDateTime.now().minusHours(1))
            .to(LocalDateTime.now().plusMinutes(1))
            .build();
    willReturn(blockedUser).given(this.blockedUserService).retrieveBlockedUserFor(eq("15"));

    // when
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    HttpEntity<?> entity = new HttpEntity<>(headers);

    // then
    Assertions.assertThrowsExactly(
        HttpClientErrorException.TooManyRequests.class,
        () -> restTemplate.exchange("/api/v1/categories", HttpMethod.GET, entity, String.class));
  }

  @Test
  void test_signin_flow() throws JsonProcessingException {
    // given
    Long userId = 15L;
    String email = "user15@yopmail.com";
    String password = "1234";
    ValidateUserRequest validateUserRequest =
        ValidateUserRequest.builder().email(email).password(password).build();
    String requestBody = new ObjectMapper().writeValueAsString(validateUserRequest);
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
    String responseBody = new ObjectMapper().writeValueAsString(validateUserResponse);
    AUTH_SERVICE.stubFor(
        post(urlEqualTo("/api/v1/users/validate"))
            .withRequestBody(equalToJson(requestBody, true, true))
            .willReturn(
                aResponse()
                    .withStatus(HttpStatus.OK.value())
                    .withHeader(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                    .withBody(responseBody)));

    // when
    HttpHeaders headers = new HttpHeaders();
    HttpEntity<?> entity = new HttpEntity<>(requestBody);
    ResponseEntity<SignInResponse> response =
        restTemplate.exchange("/api/v1/auth/signin", HttpMethod.POST, entity, SignInResponse.class);

    // then
    Assertions.assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    Assertions.assertNotNull(response.getBody());
    SignInResponse signInResponse = response.getBody();
    Assertions.assertNotNull(signInResponse.getUserid());
    Assertions.assertEquals("15", signInResponse.getUserid());
    Assertions.assertNotNull(signInResponse.getName());
    Assertions.assertNotNull(signInResponse.getToken());
    Assertions.assertFalse(jwtUtil.isInvalid(signInResponse.getToken()));
  }
}
