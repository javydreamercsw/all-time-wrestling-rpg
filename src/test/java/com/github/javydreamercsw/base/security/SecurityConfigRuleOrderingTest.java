/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.domain.account.RoleName;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Behavioral pin of the production {@code authorizeHttpRequests} ORDERING (beads ATW-fxnz): {@code
 * authorizeHttpRequests} is FIRST MATCH WINS, so {@code /api/admin/**} must be declared before the
 * broad {@code /api/**} permitAll or the admin rule is silently unreachable and the admin API is
 * effectively public.
 *
 * <p>The rules live in {@link SecurityConfig#applyAuthorizationRules} exactly because this test
 * cannot run through the bean: the {@code test} profile swaps in a permit-all chain, which would
 * make the production ordering untestable in-process. Here a bare {@link HttpSecurity} (no Spring
 * context — the same construction Spring Security's own tests use) gets the exact rules the
 * production chain registers, and requests are driven through the built {@link FilterChainProxy}. A
 * request that clears authorization reaches the terminal {@link MockFilterChain}; a denied one is
 * stopped by the 401/403 handlers first.
 */
class SecurityConfigRuleOrderingTest {

  /**
   * Bare-HttpSecurity construction shared by the chain builders: the registry resolves its path
   * matchers through the ApplicationContext (from HttpSecurity's shared objects), so a {@link
   * StaticApplicationContext} — bean-free, everything falls back to defaults — is all the
   * construction needs.
   */
  private static HttpSecurity bareHttpSecurity() {
    ObjectPostProcessor<Object> identity =
        new ObjectPostProcessor<>() {
          @Override
          public <T> T postProcess(T object) {
            return object;
          }
        };
    StaticApplicationContext context = new StaticApplicationContext();
    context.refresh();
    HashMap<Class<?>, Object> sharedObjects = new HashMap<>();
    sharedObjects.put(ApplicationContext.class, context);
    return new HttpSecurity(identity, new AuthenticationManagerBuilder(identity), sharedObjects);
  }

  /** Builds the production authorization rules into a bare HttpSecurity and returns the chain. */
  private static SecurityFilterChain productionRulesChain() throws Exception {
    HttpSecurity http = bareHttpSecurity();
    // CSRF stays at its default (enabled) — this test only issues GETs, which CSRF does not
    // guard, so no token handling is needed and the scan sees no disabled protection.
    http.authorizeHttpRequests(SecurityConfig::applyAuthorizationRules);
    // Map the outcomes this test distinguishes: anonymous denied → 401 (entry point), a
    // non-admin authenticated user denied → 403 (access denied handler), authorized → reaches
    // the terminal MockFilterChain.
    http.exceptionHandling(
        e ->
            ((ExceptionHandlingConfigurer<HttpSecurity>) e)
                .authenticationEntryPoint(
                    (req, res, ex) -> res.sendError(401, "authentication required"))
                .accessDeniedHandler((req, res, ex) -> res.sendError(403, "admin only")));
    return http.build();
  }

  /** Builds an authentication token with a single authority, SS7-safe (immutable authorities). */
  private static UsernamePasswordAuthenticationToken token(String role) {
    return new UsernamePasswordAuthenticationToken(
        "testuser", "n/a", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }

  /**
   * Drives one request through the chain. Returns the response; {@code reachedTerminal} tells
   * whether the request cleared authorization and reached the terminal filter.
   */
  private record DriveResult(MockHttpServletResponse response, boolean reachedTerminal) {}

  private static DriveResult drive(SecurityFilterChain chain, String path, Authentication auth)
      throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain terminal = new MockFilterChain();

    SecurityContextHolder.clearContext();
    if (auth != null) {
      SecurityContextHolder.getContext().setAuthentication(auth);
    }
    Filter securityFilter = new FilterChainProxy(chain);
    securityFilter.doFilter(request, response, terminal);
    SecurityContextHolder.clearContext();
    return new DriveResult(response, terminal.getRequest() != null);
  }

  @Test
  @DisplayName(
      "Anonymous request to /api/admin/** is denied (401), not passed through (beads ATW-fxnz)")
  void adminPathDeniesAnonymous() throws Exception {
    SecurityFilterChain chain = productionRulesChain();

    DriveResult result = drive(chain, "/api/admin/anything", null);

    assertTrue(
        result.response().getStatus() == 401 || result.response().getStatus() == 403,
        "/api/admin/** must NOT pass the security chain for anonymous users — got "
            + result.response().getStatus()
            + ". A 404-shaped pass-through would mean the broad /api/** permitAll shadowed"
            + " the admin rule (first-match-wins, beads ATW-fxnz)");
    assertTrue(
        !result.reachedTerminal(),
        "Anonymous admin-path request must be stopped before the endpoint");
  }

  @Test
  @DisplayName("Non-admin authenticated user gets 403 on /api/admin/**")
  void adminPathDeniesNonAdmin() throws Exception {
    SecurityFilterChain chain = productionRulesChain();

    DriveResult result = drive(chain, "/api/admin/anything", token(RoleName.VIEWER_ROLE));

    assertEquals(403, result.response().getStatus(), "Authenticated non-admin must be denied");
    assertTrue(
        !result.reachedTerminal(), "Non-admin admin-path request must never reach the endpoint");
  }

  @Test
  @DisplayName("General /api/** surface stays public-read (reaches the terminal filter)")
  void generalApiStaysPublic() throws Exception {
    SecurityFilterChain chain = productionRulesChain();

    DriveResult result = drive(chain, "/api/seasons", null);

    assertTrue(
        result.reachedTerminal(),
        "General API request must clear the security chain (public-read surface intact)");
  }

  @Test
  @DisplayName("Admin user passes the admin rule (rule present, not just denying everything)")
  void adminUserPassesAdminPath() throws Exception {
    SecurityFilterChain chain = productionRulesChain();

    DriveResult result = drive(chain, "/api/admin/anything", token(RoleName.ADMIN_ROLE));

    assertTrue(
        result.reachedTerminal(),
        "ROLE_ADMIN must clear the /api/admin/** rule and reach the endpoint — the rule must"
            + " ADMIT admins, not merely deny everyone");
  }

  @Test
  @DisplayName(
      "Shared rules must not add anyRequest — the Vaadin configurer registers matchers after"
          + " (beads ATW-8j9w)")
  void productionRegistrationOrderBuildsAfterVaadinConfigurer() throws Exception {
    // SecurityConfig.vaadinSecurityFilterChain applies VaadinSecurityConfigurer first, but the
    // configurer DEFERS its own authorizeHttpRequests contribution to build/init time — its
    // route matchers register AFTER everything declared inline. #662's anyRequest() inside
    // applyAuthorizationRules therefore sat between the inline requestMatchers and Vaadin's
    // deferred matchers, and Vaadin's registration threw "Can't configure requestMatchers
    // after anyRequest" — the docker-it production context could not start. This pin drives
    // the exact sequence: shared rules (which must stay anyRequest-free), then a deferred
    // requestMatchers registration (what the Vaadin configurer adds at init).
    HttpSecurity http = bareHttpSecurity();
    http.authorizeHttpRequests(
        auth -> {
          SecurityConfig.applyAuthorizationRules(auth); // must stay anyRequest-free
          auth.requestMatchers("/vaadin-mock/**").permitAll(); // Vaadin's deferred contribution
        });
    SecurityFilterChain chain = http.build();
    assertNotNull(chain, "shared rules + Vaadin's deferred matchers must compose");
  }
}
