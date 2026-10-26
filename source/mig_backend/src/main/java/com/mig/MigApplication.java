package com.mig;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.mig.mapper", "com.mig.auth.mapper"})
public class MigApplication {
    public static void main(String[] args) {
        SpringApplication.run(MigApplication.class, args);
    }
}
