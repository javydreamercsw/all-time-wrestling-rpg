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
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategy;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/** Security configuration for the application. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

  @Value("${security.remember-me.key:atwrpg-remember-me-key}")
  private String rememberMeKey;

  @Value("${security.remember-me.token-validity-seconds:604800}")
  private int rememberMeTokenValiditySeconds;

  // Registered here (in a @Configuration class, not @AutoConfiguration) so that Spring Security
  // 7's PrePostMethodSecurityConfiguration can @Autowired it early — before Vaadin's
  // SpringSecurityAutoConfiguration runs — preventing a ThreadLocal mismatch on ForkJoinPool
  // threads where @PreAuthorize would see an empty SecurityContext.
  //
  // We also explicitly call SecurityContextHolder.setContextHolderStrategy() so that the static
  // holder and the autowired bean share the same ThreadLocal. Without this, Spring Boot 4's
  // autoconfiguration may not register the strategy before the E2E test application context
  // initialises, leaving SecurityContextHolder on the default ThreadLocal strategy while
  // @PreAuthorize reads from the Vaadin bean — two separate ThreadLocals, so runWithContext()
  // on ForkJoinPool sets auth in one but @PreAuthorize reads empty from the other.
  @Bean
  @Primary
  public SecurityContextHolderStrategy securityContextHolderStrategy() {
    VaadinAwareSecurityContextHolderStrategy strategy =
        new VaadinAwareSecurityContextHolderStrategy();
    SecurityContextHolder.setContextHolderStrategy(strategy);
    return strategy;
  }

  @Bean
  @Profile("!test & !e2e")
  public SecurityFilterChain vaadinSecurityFilterChain(
      final HttpSecurity http, final UserDetailsService userDetailsService) {
    // 1. Apply Vaadin security configurer first
    http.with(VaadinSecurityConfigurer.vaadin(), customizer -> customizer.loginView("/login"));

    // 2. Public access to static resources and specific API endpoints
    http.authorizeHttpRequests(SecurityConfig::applyAuthorizationRules);

    // 3. Use cookie-based CSRF tokens so JavaScript/REST clients can read XSRF-TOKEN and submit it.
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

    // Configure remember-me functionality
    http.rememberMe(
        rememberMe ->
            rememberMe
                .key(rememberMeKey)
                .tokenValiditySeconds(rememberMeTokenValiditySeconds)
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

  /**
   * The authorization rules for the production chain, in declaration order — FIRST MATCH WINS in
   * {@code authorizeHttpRequests}, so the ordering here is a security invariant: specific rules
   * MUST precede broad catch-alls, or they are silently unreachable. {@code /api/admin/**} is
   * declared BEFORE {@code /api/**} so the admin API requires ADMIN even though the general API
   * surface is public-read (pinned by {@code SecurityConfigRuleOrderingTest}).
   *
   * <p>Extracted as a static method so the ordering test can exercise the exact rules the
   * production chain registers — the test profile swaps in a permit-all chain, which would
   * otherwise make this ordering untestable in-process.
   */
  static void applyAuthorizationRules(
      AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
          auth) {
    // Specific admin rule FIRST — a later rule can never override an earlier broad one.
    auth.requestMatchers("/api/admin/**").hasRole(RoleName.ADMIN_ROLE);
    // Static resources, the general API surface (public-read), and auth pages.
    auth.requestMatchers(
            "/images/**",
            "/challenge-content/**",
            "/icons/**",
            "/public/**",
            "/api/**",
            "/docs/**",
            "/api/auth/**",
            "/login",
            "/register",
            "/health",
            "/api/system/health")
        .permitAll();
    // NO anyRequest() here: the production chain applies VaadinSecurityConfigurer first, and
    // its route-access catch-all already registers an anyRequest() rule — any requestMatchers
    // added after that throw "Can't configure requestMatchers after anyRequest" (the docker-it
    // startup failure of beads ATW-8j9w). Non-API paths fall through to Vaadin's route-based
    // access control, as they did before #662.
  }

  @Bean
  @Profile("test & !e2e")
  @Order(0)
  public SecurityFilterChain testSecurityFilterChain(final HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

    return http.build();
  }
}
