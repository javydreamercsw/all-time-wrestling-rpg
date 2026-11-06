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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Wires the committed seed-provenance manifest and the REST mutation guard into the seed-update
 * review feature. Loading is lenient: when the manifest resource is absent or malformed (e.g.
 * before the generation task has ever run), the bean is an empty manifest and seed-review detection
 * simply degrades — startup never breaks.
 */
@Configuration
public class SeedReviewConfig {

  @Bean
  public SeedProvenanceManifest seedProvenanceManifest(ObjectMapper objectMapper) {
    return SeedProvenanceManifest.load(objectMapper);
  }

  /**
   * Registers the REST mutation guard on the app's REST surface ({@code /api/*} — every REST
   * controller in this codebase maps there) at {@link Ordered#LOWEST_PRECEDENCE}, i.e. after the
   * Spring Security filter chain. This project declares {@code spring-security-*} directly instead
   * of Boot's {@code spring-boot-security} module, so security's chain has no named order constant
   * to reference; lowest precedence guarantees the guard runs downstream of it either way. URL
   * patterns (rather than a request matcher) keep Vaadin's own endpoints — guarded by Task 7's
   * navigation freeze — outside this filter entirely.
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
