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

package io.oigres.ecomm.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure kafka topic when this gateway must send and receive messages.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Data
@ConfigurationProperties(prefix = "ecomm.service.limiter")
public class LimiterServiceProperties {

  @Data
  public static class IncomingRequestTopicProperties {
    @NotNull @NotBlank private String name;
    private int partitions;
    private short replicationFactor;
  }

  @Data
  public static class BlacklistedUsersTopicProperties {
    @NotNull @NotBlank private String name;
    private int concurrency;
  }

  @Data
  public static class Topics {
    @NotNull @NotBlank private IncomingRequestTopicProperties incomingRequest;
    @NotNull @NotBlank private BlacklistedUsersTopicProperties blacklistedUsers;
  }

  @NotNull private Topics topics;
}
