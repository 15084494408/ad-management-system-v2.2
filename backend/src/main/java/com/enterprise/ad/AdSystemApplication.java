package com.enterprise.ad;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.enterprise.ad.module.**.mapper")
public class AdSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdSystemApplication.class, args);
    }
}
