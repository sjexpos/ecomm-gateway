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

package io.oigres.ecomm.gateway.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO class which will be stored in cache when the user reaches its rate limit.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUser {
  private String userId;
  private LocalDateTime from;
  private LocalDateTime to;

  public boolean isBlock(LocalDateTime time) {
    return !time.isBefore(getFrom()) && !time.isAfter(getTo());
  }
}
