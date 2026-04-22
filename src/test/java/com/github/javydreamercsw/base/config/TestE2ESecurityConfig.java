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
package com.github.javydreamercsw.base.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.security.WithCustomMockUserSecurityContextFactory;
import com.github.javydreamercsw.management.config.InboxEventTypeConfig;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.Optional;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for E2E tests. Provides a simplified security setup that allows the tests
 * to interact with the UI without complex authentication logic.
 */
@TestConfiguration
@EnableWebSecurity
@Profile("e2e")
@Import({WithCustomMockUserSecurityContextFactory.class, InboxEventTypeConfig.class})
public class TestE2ESecurityConfig {

  @Bean
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    // For E2E tests, we want to allow everything so the Selenium driver can interact with the UI
    // We avoid VaadinSecurityConfigurer to prevent "Vaadin servlet url mapping is required" errors
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

    http.csrf(csrf -> csrf.disable());
    http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

    // Basic form login for the "login" action used in LoginView
    http.formLogin(
        form ->
            form.loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/?continue", true)
                .permitAll());

    // Ensure logout redirects to login page
    http.logout(
        logout ->
            logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public AuthenticationContext authenticationContext() {
    // Return a mocked AuthenticationContext for E2E tests to avoid dependency issues
    // and NullPointerExceptions during logout
    AuthenticationContext mock = Mockito.mock(AuthenticationContext.class);

    // Mock common behavior used in SecurityUtils
    Mockito.when(mock.isAuthenticated())
        .thenAnswer(inv -> SecurityContextHolder.getContext().getAuthentication() != null);

    Mockito.when(mock.getAuthenticatedUser(Mockito.any()))
        .thenAnswer(
            inv -> {
              Class<?> type = inv.getArgument(0);
              Authentication auth = SecurityContextHolder.getContext().getAuthentication();
              if (auth != null && type.isInstance(auth.getPrincipal())) {
                return Optional.of(type.cast(auth.getPrincipal()));
              }
              return Optional.empty();
            });

    // Handle logout safely
    Mockito.doAnswer(
            inv -> {
              SecurityContextHolder.clearContext();
              return null;
            })
        .when(mock)
        .logout();

    return mock;
  }
}
