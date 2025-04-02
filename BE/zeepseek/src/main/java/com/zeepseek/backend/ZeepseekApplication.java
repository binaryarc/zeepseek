package com.zeepseek.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ZeepseekApplication {

	public static void main(String[] args) {

		SpringApplication.run(ZeepseekApplication.class, args);
	}

}
