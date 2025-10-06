package org.sav.fornas.bff;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class BffApplication {

	public static void main(String[] args) {
		log.info("------------------------------------<Starting>--------------------------------------");
		SpringApplication.run(BffApplication.class, args);
		log.info("------------------------------------<Started>--------------------------------------");
	}

}
