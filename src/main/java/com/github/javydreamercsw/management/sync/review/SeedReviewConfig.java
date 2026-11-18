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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Wires the committed seed-provenance manifest, the REST mutation guard, and the force-resolve
 * escape hatch into the seed-update review feature. Loading is lenient: when the manifest resource
 * is absent or malformed (e.g. before the generation task has ever run), the bean is an empty
 * manifest and seed-review detection simply degrades — startup never breaks.
 */
@Configuration
@Slf4j
public class SeedReviewConfig {

  /**
   * The only accepted {@code seed.review.force-resolve} value — force-decide everything as
   * keep-mine.
   */
  public static final String FORCE_RESOLVE_KEEP_ALL = "keep-all";

  /**
   * Operator escape hatch: {@code seed.review.force-resolve=keep-all}. Empty by default — the hatch
   * never engages unless the property is set explicitly. Read into a field here so {@link
   * #forceResolveIfConfigured()} stays callable without reflection-property plumbing.
   */
  @Value("${seed.review.force-resolve:}")
  private String forceResolve;

  private final SeedUpdateReviewService reviewService;

  public SeedReviewConfig(SeedUpdateReviewService reviewService) {
    this.reviewService = reviewService;
  }

  /**
   * Operator escape hatch for the seed-review gate: when {@code seed.review.force-resolve=keep-all}
   * is set at boot and PENDING decisions exist, delegates to {@code
   * SeedUpdateReviewService.forceResolvePendingAsKeepMine()} — every pending row is force-decided
   * as keep-mine and the gate lowers, logged WARN.
   *
   * <p><b>This is an operator escape, not a real apply.</b> Keep-mine means nothing changes in the
   * database — but unlike the review UI's apply flow, this hatch does NOT re-stamp the seed
   * fingerprints of the decided rows. The rows re-collide on the next upgrade boot (fail-safe
   * direction: the operator is asked again, nothing is silently adopted).
   */
  public void forceResolveIfConfigured() {
    if (FORCE_RESOLVE_KEEP_ALL.equals(forceResolve)) {
      reviewService.forceResolvePendingAsKeepMine();
    } else if (forceResolve != null && !forceResolve.isBlank()) {
      log.warn(
          "Unsupported seed.review.force-resolve value '{}' — only '{}' is recognized."
              + " Pending seed decisions are left untouched.",
          forceResolve,
          FORCE_RESOLVE_KEEP_ALL);
    }
  }

  @Bean
  public SeedProvenanceManifest seedProvenanceManifest(ObjectMapper objectMapper) {
    return SeedProvenanceManifest.load(objectMapper);
  }

  /**
   * Registers the REST mutation guard on the app's REST surface ({@code /api/*} — every REST
   * controller in this codebase maps there) at {@link Ordered#LOWEST_PRECEDENCE}. Order note: this
   * project declares {@code spring-security-*} directly instead of Boot's {@code
   * spring-boot-security} module, so the security chain ({@code springSecurityFilterChain}) is a
   * plain {@code Filter} bean that Boot auto-registers at the same default order — {@link
   * Ordered#LOWEST_PRECEDENCE}. The tie resolves guard-first (explicit registrations sort before
   * adapted beans), which is acceptable here because {@code /api/**} is {@code permitAll} in {@code
   * SecurityConfig}: there is nothing to authenticate before guarding. The only visible effect is
   * that during a freeze an unauthenticated probe of {@code /api/admin/**} gets the guard's 503
   * instead of the auth failure it would otherwise see. Deliberately NOT adding
   * DelegatingFilterProxy ordering machinery to force a strict after-security position — that would
   * be overkill for a permitAll surface. URL patterns (rather than a request matcher) keep Vaadin's
   * own endpoints — guarded by Task 7's navigation freeze — outside this filter entirely.
   */
  @Bean
  public FilterRegistrationBean<SeedReviewRestGuard> seedReviewRestGuard(SeedReviewGate gate) {
    FilterRegistrationBean<SeedReviewRestGuard> registration =
        new FilterRegistrationBean<>(new SeedReviewRestGuard(gate));
    registration.setName("seedReviewRestGuard");
    registration.addUrlPatterns("/api/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.LOWEST_PRECEDENCE);
    return registration;
  }
}
