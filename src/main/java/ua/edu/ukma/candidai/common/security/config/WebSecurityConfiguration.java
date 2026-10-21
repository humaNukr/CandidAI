package ua.edu.ukma.candidai.common.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Security filter chain for browser interface (Thymeleaf views and static web resources).
 * Handles all requests not intercepted by /api/** (JWT) or OpenAPI chains.
 */
@Configuration(proxyBeanMethods = false)
public class WebSecurityConfiguration {

    private static final int CHAIN_ORDER = 3;

    @Bean
    @Order(CHAIN_ORDER)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/css/**",
                                "/webjars/**",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/vacancies").permitAll()
                        .requestMatchers("/vacancies/*/apply").hasRole("CANDIDATE")
                        .requestMatchers("/vacancies/*/applications").hasAnyRole("TECH_LEAD", "RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/vacancies/new").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/vacancies").hasAnyRole("RECRUITER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/vacancies/*").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/vacancies", false))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("CANDIDAI_SESSION_ID", "JSESSIONID"))
                .sessionManagement(session -> session
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::migrateSession)
                        .maximumSessions(1))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; img-src 'self' data:; "
                                        + "script-src 'self'; style-src 'self'; frame-ancestors 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
