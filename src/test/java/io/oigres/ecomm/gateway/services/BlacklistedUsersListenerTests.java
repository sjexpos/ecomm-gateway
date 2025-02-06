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

package io.oigres.ecomm.gateway.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import io.oigres.ecomm.service.limiter.BlackedInfo;
import java.time.LocalDateTime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class BlacklistedUsersListenerTests {

  @TestConfiguration
  @ComponentScan
  static class TestConfig {}

  @MockBean BlockedUserService blockedUserService;
  @Autowired BlacklistedUsersListener blacklistedUsersListener;

  @Test
  void test_default_flow() {
    // given
    String userId = "user14@yopmail.com";
    LocalDateTime from = LocalDateTime.now().minusHours(2);
    LocalDateTime to = LocalDateTime.now();
    BlackedInfo blackedInfo = BlackedInfo.builder().userId(userId).from(from).to(to).build();
    ConsumerRecord<String, BlackedInfo> record =
        new ConsumerRecord<>("my_topic", 1, 15, userId, blackedInfo);
    willDoNothing().given(this.blockedUserService).processBlackedInfo(any());

    // when
    blacklistedUsersListener.consumeMessage(record);

    // then
    ArgumentCaptor<BlackedInfo> blackedInfoCaptor = ArgumentCaptor.forClass(BlackedInfo.class);
    then(this.blockedUserService).should().processBlackedInfo(blackedInfoCaptor.capture());
    Assertions.assertNotNull(blackedInfoCaptor.getValue());
    Assertions.assertEquals(userId, blackedInfoCaptor.getValue().getUserId());
    Assertions.assertEquals(from, blackedInfoCaptor.getValue().getFrom());
    Assertions.assertEquals(to, blackedInfoCaptor.getValue().getTo());
  }

  @Test
  void test_null_blacked_info_flow() {
    // given
    String userId = "user14@yopmail.com";
    ConsumerRecord<String, BlackedInfo> record =
        new ConsumerRecord<>("my_topic", 1, 15, userId, null);
    willDoNothing().given(this.blockedUserService).processBlackedInfo(any());

    // when
    blacklistedUsersListener.consumeMessage(record);

    // then
    then(this.blockedUserService).shouldHaveNoInteractions();
  }

  @Test
  void test_not_catch_exception_flow() {
    // given
    String userId = "user14@yopmail.com";
    LocalDateTime from = LocalDateTime.now().minusHours(2);
    LocalDateTime to = LocalDateTime.now();
    BlackedInfo blackedInfo = BlackedInfo.builder().userId(userId).from(from).to(to).build();
    ConsumerRecord<String, BlackedInfo> record =
        new ConsumerRecord<>("my_topic", 1, 15, userId, blackedInfo);
    willThrow(new RuntimeException()).given(this.blockedUserService).processBlackedInfo(any());

    // when
    blacklistedUsersListener.consumeMessage(record);

    // then
    ArgumentCaptor<BlackedInfo> blackedInfoCaptor = ArgumentCaptor.forClass(BlackedInfo.class);
    then(this.blockedUserService).should().processBlackedInfo(blackedInfoCaptor.capture());
    Assertions.assertNotNull(blackedInfoCaptor.getValue());
    Assertions.assertEquals(userId, blackedInfoCaptor.getValue().getUserId());
    Assertions.assertEquals(from, blackedInfoCaptor.getValue().getFrom());
    Assertions.assertEquals(to, blackedInfoCaptor.getValue().getTo());
  }
}
