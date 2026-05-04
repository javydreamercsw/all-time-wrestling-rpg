/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.base.security;

import com.github.javydreamercsw.base.domain.account.RoleName;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/** Security configuration for the application. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final UserDetailsService userDetailsService;

  @Bean
  @Profile("!test & !e2e")
  public SecurityFilterChain vaadinSecurityFilterChain(HttpSecurity http) throws Exception {
    // Public access to static resources and specific API endpoints
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers(
                    "/images/**",
                    "/icons/**",
                    "/public/**",
                    "/api/**",
                    "/docs/**",
                    "/api/auth/**",
                    "/login",
                    "/register",
                    "/health",
                    "/api/system/health")
                .permitAll()
                .requestMatchers("/api/admin/**")
                .hasRole(RoleName.ADMIN_ROLE)
                .anyRequest()
                .authenticated());

    // Use cookie-based CSRF tokens so JavaScript/REST clients can read XSRF-TOKEN and submit it.
    // The deferred handler avoids the Spring Security 6 double-read issue with Vaadin.
    CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName(null);
    http.csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers("/api/**", "/h2-console/**"));

    // Allow framing for H2 console
    http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

    // Apply Vaadin security configurer and set the login view
    http.with(VaadinSecurityConfigurer.vaadin(), customizer -> customizer.loginView("/login"));

    // Configure remember-me functionality
    http.rememberMe(
        rememberMe ->
            rememberMe
                .key("atwrpg-remember-me-key") // Should be externalized to properties
                .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days
                .userDetailsService(userDetailsService));

    // Configure logout
    http.logout(
        logout ->
            logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll());

    // Configure form login
    http.formLogin(AbstractAuthenticationFilterConfigurer::permitAll);

    // Add security headers
    http.headers(
        headers ->
            headers.httpStrictTransportSecurity(
                hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)));

    return http.build();
  }

  @Bean
  @Profile("test")
  @Order(0)
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

    return http.build();
  }
}
