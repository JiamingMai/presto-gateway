package com.kapok;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@ServletComponentScan(basePackages = {"com.kapok.filter"})
@MapperScan(basePackages = {"com.kapok.dao"})
@SpringBootApplication
@EnableScheduling
public class PrestoGatewayApplication {

    @Bean(name = "restTemplate")
    public RestTemplate initRestTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(PrestoGatewayApplication.class, args);
    }

}
