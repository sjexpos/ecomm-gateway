package io.oigres.ecomm.gateway.services;

import io.oigres.ecomm.gateway.model.BlockedUser;
import io.oigres.ecomm.service.limiter.BlackedInfo;

public interface BlockedUserService {

    void processBlackedInfo(BlackedInfo info);

    BlockedUser retrieveBlockedUserFor(String userId);

}
