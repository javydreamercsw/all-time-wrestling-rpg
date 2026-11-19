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
package com.github.javydreamercsw.management.sync.review;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UrlPathHelper;

/**
 * REST-layer half of the seed-update review gate (Task 6): freezes the app's JSON REST surface
 * while seed decisions are pending.
 *
 * <p>When {@link SeedReviewGate#isUp()}, every mutating verb (POST/PUT/PATCH/DELETE) on a guarded
 * path is answered 503 with body {@code {"error":"seed review pending"}}; read verbs (GET/HEAD/
 * OPTIONS) pass untouched, as do all verbs when the gate is down. Admins are deliberately NOT
 * exempted here — the admin review flow lives in the Vaadin UI (Task 7), not at the REST level, so
 * the freeze cannot be lifted over the same REST surface it protects.
 *
 * <p>Scope: this guard covers the app's REST surface only. In this codebase every REST controller
 * maps under {@code /api/**} (class-level {@code @RequestMapping("/api/...")}), so that prefix
 * stands in for the brief's "/rest/**". Vaadin navigation and static resources are NOT this
 * filter's concern — the navigation freeze is Task 7's Vaadin navigation observer.
 *
 * <p>Exempt paths exist for the review UI's own REST surface (PR 3), which does not exist yet —
 * {@link #EXEMPT_PATHS} is empty until then, structured so PR 3 only needs to add entries.
 */
public class SeedReviewRestGuard implements Filter {

  /**
   * REST path patterns (Ant style, matched against the path relative to the context root) that stay
   * writable even while the gate is up. Empty until PR 3 adds the review UI's REST endpoints (e.g.
   * {@code /api/seed-review/**}); the exemption mechanism is pinned by {@code
   * SeedReviewRestGuardTest.exemptPathsIncludeReviewEndpoints}.
   */
  public static final Set<String> EXEMPT_PATHS = Set.of();

  private static final String ERROR_BODY = "{\"error\":\"seed review pending\"}";

  private static final String REST_PREFIX = "/api/";

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  /**
   * Same path resolution the Spring handler mappings use: decodes the (raw, container-undecoded)
   * request URI and strips the context path. Matching on this lookup path closes the encoded-path
   * bypass — {@code POST /%61pi/...} maps to the {@code /api/...} handler on the container, and
   * only a decoded comparison sees that.
   */
  private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();

  private final SeedReviewGate gate;

  private final Set<String> exemptPaths;

  /** Production constructor: PR 3's review endpoints will extend {@link #EXEMPT_PATHS}. */
  public SeedReviewRestGuard(SeedReviewGate gate) {
    this(gate, EXEMPT_PATHS);
  }

  /** Test/extension constructor with an explicit exemption set. */
  public SeedReviewRestGuard(SeedReviewGate gate, Set<String> exemptPaths) {
    this.gate = gate;
    this.exemptPaths = exemptPaths;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (!(request instanceof HttpServletRequest httpRequest)
        || !(response instanceof HttpServletResponse httpResponse)) {
      chain.doFilter(request, response);
      return;
    }

    String path = PATH_HELPER.getPathWithinApplication(httpRequest);
    if (!path.startsWith(REST_PREFIX) || isExempt(path)) {
      chain.doFilter(request, response);
      return;
    }

    if (!isMutatingVerb(httpRequest.getMethod()) || !gate.isUp()) {
      chain.doFilter(request, response);
      return;
    }

    httpResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
    httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
    httpResponse.getWriter().write(ERROR_BODY);
  }

  private boolean isExempt(String path) {
    return exemptPaths.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
  }

  private boolean isMutatingVerb(String method) {
    return switch (method) {
      case "POST", "PUT", "PATCH", "DELETE" -> true;
      default -> false;
    };
  }
}
