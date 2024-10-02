package io.oigres.ecomm.gateway.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

public class BlockedUserTest {
    static private ObjectMapper mapper;

    @BeforeAll
    static void setup() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void test_serialize_deserialize() throws JsonProcessingException {

        // given
        LocalDateTime from = LocalDateTime.of(2024, 3, 10, 12, 15, 30, 0);
        LocalDateTime to = LocalDateTime.of(2024, 3, 10, 13, 15, 30, 0);
        BlockedUser blockedUser = BlockedUser.builder()
                .userId("123")
                .from(from)
                .to(to)
                .build();

        // when
        String json = mapper.writeValueAsString(blockedUser);
        // then
        Assertions.assertNotNull(json);

        // when
        BlockedUser deserialized = mapper.readValue(json, BlockedUser.class);
        // then
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(blockedUser.getUserId(), deserialized.getUserId());
        Assertions.assertEquals(from, deserialized.getFrom());
        Assertions.assertEquals(to, deserialized.getTo());
    }

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
                Arguments.of(LocalDateTime.of(2024, 3, 10, 12, 45, 30, 0), true),
                Arguments.of(LocalDateTime.of(2024, 3, 10, 12, 15, 30, 0), true),
                Arguments.of(LocalDateTime.of(2024, 3, 10, 13, 15, 30, 0), true),
                Arguments.of(LocalDateTime.of(2024, 3, 10, 12, 15, 29, 0), false),
                Arguments.of(LocalDateTime.of(2024, 3, 10, 13, 15, 31, 0), false),
                Arguments.of(LocalDateTime.of(2024, 3, 10, 12, 10, 29, 0), false),
                Arguments.of(LocalDateTime.of(2024, 3, 10, 13, 25, 31, 0), false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    void test_is_blocked(LocalDateTime test, boolean result) {
        // given
        LocalDateTime from = LocalDateTime.of(2024, 3, 10, 12, 15, 30, 0);
        LocalDateTime to = LocalDateTime.of(2024, 3, 10, 13, 15, 30, 0);
        BlockedUser blockedUser = BlockedUser.builder()
                .userId("123")
                .from(from)
                .to(to)
                .build();

        // when
        boolean status = blockedUser.isBlock(test);

        // then
        Assertions.assertEquals(result, status);
    }

}
