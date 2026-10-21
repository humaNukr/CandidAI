package ua.edu.ukma.candidai.common.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestWebSecurityConfiguration {

    @Bean
    @Order(1)
    public SecurityFilterChain testWebFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/css/**", "/webjars/**", "/vacancies/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/webjars/**").permitAll()
                        .requestMatchers("/vacancies/**").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
