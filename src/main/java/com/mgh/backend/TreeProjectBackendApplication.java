package com.mgh.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TreeProjectBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TreeProjectBackendApplication.class, args);
	}

}
