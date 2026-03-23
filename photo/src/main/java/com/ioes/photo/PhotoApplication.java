package com.ioes.photo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Photo 서비스 애플리케이션
 * @author 황제연
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PhotoApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhotoApplication.class, args);
	}

}
