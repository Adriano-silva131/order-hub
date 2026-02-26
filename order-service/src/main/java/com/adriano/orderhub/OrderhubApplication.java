package com.adriano.orderhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OrderhubApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderhubApplication.class, args);
	}

}
