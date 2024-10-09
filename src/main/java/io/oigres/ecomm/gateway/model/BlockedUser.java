package io.oigres.ecomm.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
