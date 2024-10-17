package io.oigres.ecomm.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import reactor.core.publisher.Hooks;

/**
 * Bootstrap spring boot class.
 *
 * @author sergio.exposito (sjexpos@gmail.com)
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayApplication {

	public static void main(String[] args) {
		System.out.println("--------------------------------------------------------------");
		System.out.println(System.getProperty("spring.kafka.properties.sasl.jaas.config"));
		System.out.println("--------------------------------------------------------------");
		Hooks.enableAutomaticContextPropagation();
		SpringApplication.run(GatewayApplication.class, args);
	}

}
