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

import io.oigres.ecomm.gateway.model.BlockedUser;
import io.oigres.ecomm.service.limiter.BlackedInfo;

/**
 * Service class to store and retrieve blocked users
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
public interface BlockedUserService {

  /**
   * Stores information about a blacklisted user.
   *
   * @param info
   */
  void processBlackedInfo(BlackedInfo info);

  /**
   * Retrieves information about rate limit violation by userid. If there is not a violation, this
   * method will return null.
   *
   * @param userId an user identifier
   * @return rate limit violation if the user has one, null otherwise.
   */
  BlockedUser retrieveBlockedUserFor(String userId);
}
