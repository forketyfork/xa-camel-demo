package com.forketyfork.xacameldemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
public class XaCamelDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(XaCamelDemoApplication.class, args);
    }

}
