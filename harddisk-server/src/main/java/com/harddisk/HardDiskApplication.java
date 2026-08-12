package com.harddisk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.harddisk.module.*.mapper")
public class HardDiskApplication {
    public static void main(String[] args) {
        SpringApplication.run(HardDiskApplication.class, args);
    }
}
