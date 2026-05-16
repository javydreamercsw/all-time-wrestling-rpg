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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for E2E tests. Uses Vaadin's SpringSecurityAutoConfiguration to wire
 * VaadinAwareSecurityContextHolderStrategy, but provides a simplified filter chain.
 */
@TestConfiguration
@EnableWebSecurity
@Profile("e2e")
@Import({InboxEventTypeConfig.class})
@Slf4j
public class TestE2ESecurityConfig {

  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules();
    return mapper;
  }

  /**
   * Register the factory bean explicitly so @WithCustomMockUser can find it during test class
   * loading. This ensures Spring's @WithSecurityContext annotation processing succeeds.
   */
  @Bean
  public WithCustomMockUserSecurityContextFactory withCustomMockUserSecurityContextFactory() {
    return new WithCustomMockUserSecurityContextFactory();
  }

  @Bean
  public SecurityFilterChain testSecurityFilterChain(
      final HttpSecurity http, final AuthenticationContext authenticationContext) throws Exception {
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers(
                    "/login",
                    "/login/**",
                    "/images/**",
                    "/icons/**",
                    "/public/**",
                    "/api/**",
                    "/docs/**",
                    "/VAADIN/**",
                    "/line-awesome/**",
                    "/frontend/**")
                .permitAll()
                .anyRequest()
                .permitAll());

    http.csrf(AbstractHttpConfigurer::disable);
    http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

    http.formLogin(
        form ->
            form.loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(
                    (req, res, auth) -> {
                      log.debug(
                          "[E2E] Login SUCCESS: user={}, authorities={}",
                          auth.getName(),
                          auth.getAuthorities());
                      res.sendRedirect("/atw-rpg/");
                    })
                .failureHandler(
                    (req, res, ex) -> {
                      log.error(
                          "[E2E] Login FAILED: user={}, reason={}",
                          req.getParameter("username"),
                          ex.getMessage());
                      res.sendRedirect("/atw-rpg/login?error");
                    })
                .permitAll());

    http.logout(
        logout ->
            logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll());

    // Build first, then wire AuthenticationContext — applySecurityConfiguration calls
    // http.getObject() which requires the chain to already be built.
    SecurityFilterChain chain = http.build();
    AuthenticationContext.applySecurityConfiguration(http, authenticationContext);
    return chain;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }
}
