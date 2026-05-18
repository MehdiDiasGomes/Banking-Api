package com.mehdi.banking_api.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {

    @Bean
    public Resend resendClient(@Value("${resend.api-key}") String apiKey) {
        return new Resend(apiKey);
    }
}
