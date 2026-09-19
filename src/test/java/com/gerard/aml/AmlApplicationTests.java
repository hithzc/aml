package com.gerard.aml;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "aml.rules.file=classpath:valid-rules.json")
class AmlApplicationTests {

	@Test
	void contextLoads() {
	}

}
