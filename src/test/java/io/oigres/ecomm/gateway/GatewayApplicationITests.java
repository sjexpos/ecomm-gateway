package io.oigres.ecomm.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
		locations = "classpath:application-integrationtest.properties"
)
class GatewayApplicationITests {

	@Test
	void contextLoads() {
	}

}
