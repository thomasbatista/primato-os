package com.primatoos.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PrimatoOsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrimatoOsBackendApplication.class, args);
	}

}
