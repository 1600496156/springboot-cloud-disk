package com.mhc.springbootclouddisk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class SpringbootCloudDiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootCloudDiskApplication.class, args);
    }

}
