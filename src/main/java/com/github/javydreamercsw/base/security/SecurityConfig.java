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

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/** Security configuration for the application. */
@Configuration
@EnableWebSecurity
@Profile("!test & !e2e")
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomUserDetailsService userDetailsService;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // Public access to static resources
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/images/**", "/icons/**", "/public/**", "/api/**").permitAll());

    // Disable CSRF for API endpoints
    http.csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**")));

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

    return http.build();
  }

  /**
   * Password encoder bean. Uses BCrypt with strength 10.
   *
   * @return the password encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }
}
