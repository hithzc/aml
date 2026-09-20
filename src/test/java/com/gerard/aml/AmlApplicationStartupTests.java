package com.gerard.aml;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AmlApplicationStartupTests {

	@Test
	void startsSuccessfullyWithValidRules() {
		ConfigurableApplicationContext ctx = new SpringApplicationBuilder(AmlApplication.class)
				.properties("RULES_FILE=classpath:valid-rules.json")
				.run();
		ctx.close();
	}

	@Test
	void failsToStartWithInvalidRules() {
		assertThrows(Exception.class, () -> {
			new SpringApplicationBuilder(AmlApplication.class)
					.properties("RULES_FILE=classpath:invalid-rules.json")
					.run();
		});
	}
}