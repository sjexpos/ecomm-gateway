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
     * Retrieves information about rate limit violation by userid. If there is not a violation, this method will return null.
     *
     * @param userId an user identifier
     * @return rate limit violation if the user has one, null otherwise.
     */
    BlockedUser retrieveBlockedUserFor(String userId);

}
