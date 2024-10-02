package io.oigres.ecomm.gateway.model;

import io.oigres.ecomm.service.limiter.BlackedInfo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BlackInfoBlockedUserMapper {

    BlockedUser from(BlackedInfo blackedInfo);

}
