package com.darum.employee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ApplicationConfig {

    @Bean
    public RestClient restTemplate(RestClient.Builder restClientBuilder) {
        return restClientBuilder.build();
    }
}
