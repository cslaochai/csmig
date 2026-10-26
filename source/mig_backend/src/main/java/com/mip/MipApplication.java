package com.mip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mip.mapper")
public class MipApplication {
    public static void main(String[] args) {
        SpringApplication.run(MipApplication.class, args);
    }
}
