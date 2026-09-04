package com.petplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.petplatform.module")
public class PetPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetPlatformApplication.class, args);
    }
}
