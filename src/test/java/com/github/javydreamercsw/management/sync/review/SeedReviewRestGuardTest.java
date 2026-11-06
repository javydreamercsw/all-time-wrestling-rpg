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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Behavior table for the REST mutation guard (seed-update review feature, Task 6).
 *
 * <p>Standalone MockMvc with a probe controller plus a mocked {@link SeedReviewGate} is the
 * lightest honest harness here: the contract is pure filter behavior (status/body/content-type per
 * verb and gate state), so a full application context would only add security noise that the guard
 * deliberately does not depend on. The gate is a mock for the same reason — {@code
 * SeedReviewGateTest}-shaped persistence behavior is already covered elsewhere.
 */
class SeedReviewRestGuardTest {

  private static final String PROBE_PATH = "/api/probe";

  /** Stands in for the review UI's REST surface that PR 3 will add. */
  private static final String SEED_REVIEW_PATH = "/api/seed-review/decisions";

  private static final String EXPECTED_ERROR_BODY = "{\"error\":\"seed review pending\"}";

  private SeedReviewGate gate;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    gate = mock(SeedReviewGate.class);
  }

  private MockMvc mockMvcWithGuard(SeedReviewRestGuard guard) {
    return MockMvcBuilders.standaloneSetup(new ProbeController()).addFilters(guard).build();
  }

  @Test
  void blocksMutatingVerbsWhenGateUp() throws Exception {
    when(gate.isUp()).thenReturn(true);
    mockMvc = mockMvcWithGuard(new SeedReviewRestGuard(gate));
    for (var request :
        List.of(post(PROBE_PATH), put(PROBE_PATH), patch(PROBE_PATH), delete(PROBE_PATH))) {
      mockMvc
          .perform(request)
          .andExpect(status().isServiceUnavailable())
          .andExpect(content().string(EXPECTED_ERROR_BODY));
    }
  }

  @Test
  void allowsReadVerbsWhenGateUp() throws Exception {
    when(gate.isUp()).thenReturn(true);
    mockMvc = mockMvcWithGuard(new SeedReviewRestGuard(gate));
    for (var request : List.of(get(PROBE_PATH), head(PROBE_PATH), options(PROBE_PATH))) {
      mockMvc.perform(request).andExpect(status().isOk());
    }
  }

  @Test
  void passesAllVerbsWhenGateDown() throws Exception {
    when(gate.isUp()).thenReturn(false);
    mockMvc = mockMvcWithGuard(new SeedReviewRestGuard(gate));
    for (var request :
        List.of(
            get(PROBE_PATH),
            head(PROBE_PATH),
            options(PROBE_PATH),
            post(PROBE_PATH),
            put(PROBE_PATH),
            patch(PROBE_PATH),
            delete(PROBE_PATH))) {
      mockMvc.perform(request).andExpect(status().isOk());
    }
  }

  @Test
  void exemptPathsIncludeReviewEndpoints() throws Exception {
    // Production set stays empty until PR 3 adds the review UI's REST surface; the mechanism is
    // pinned here so that adding an entry to EXEMPT_PATHS is all PR 3 needs to do.
    assertTrue(
        SeedReviewRestGuard.EXEMPT_PATHS.isEmpty(),
        "No review REST surface exists yet; PR 3 populates EXEMPT_PATHS");
    SeedReviewRestGuard guard = new SeedReviewRestGuard(gate, Set.of("/api/seed-review/**"));
    mockMvc = mockMvcWithGuard(guard);
    when(gate.isUp()).thenReturn(true);
    // Exempt path: writable even with the gate up.
    mockMvc.perform(post(SEED_REVIEW_PATH)).andExpect(status().isOk());
    // Non-exempt REST path: still frozen.
    mockMvc.perform(post(PROBE_PATH)).andExpect(status().isServiceUnavailable());
  }

  @Test
  void responseBodyIsExactJson() throws Exception {
    when(gate.isUp()).thenReturn(true);
    mockMvc = mockMvcWithGuard(new SeedReviewRestGuard(gate));
    mockMvc
        .perform(post(PROBE_PATH))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string("{\"error\":\"seed review pending\"}"));
  }

  @Test
  void contentTypeIsApplicationJson() throws Exception {
    when(gate.isUp()).thenReturn(true);
    mockMvc = mockMvcWithGuard(new SeedReviewRestGuard(gate));
    mockMvc
        .perform(post(PROBE_PATH))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
  }

  @Test
  void nonRestPathsPassThroughWhenGateUp() throws Exception {
    when(gate.isUp()).thenReturn(true);
    mockMvc = mockMvcWithGuard(new SeedReviewRestGuard(gate));
    // Stands in for Vaadin's own endpoints: the navigation freeze is Task 7's job, not this
    // filter's.
    mockMvc.perform(post("/vaadin-uidl")).andExpect(status().isOk());
  }

  @Test
  void filterRegistrationCoversRestSurfaceWithDocumentedOrder() {
    FilterRegistrationBean<SeedReviewRestGuard> registration =
        new SeedReviewConfig().seedReviewRestGuard(gate);
    assertEquals(SeedReviewRestGuard.class, registration.getFilter().getClass());
    // Boot 4's DynamicRegistrationBean has setName() but no getName() accessor; the label is
    // cosmetic, so only the behavioral parts of the registration are pinned here.
    assertTrue(
        registration.getUrlPatterns().contains("/api/*"), "All REST controllers map under /api/**");
    assertEquals(Ordered.LOWEST_PRECEDENCE, registration.getOrder());
  }

  /** Minimal REST surface the guard is exercised against. */
  @RestController
  static class ProbeController {

    @GetMapping(PROBE_PATH)
    String getProbe() {
      return "get-ok";
    }

    @PostMapping(PROBE_PATH)
    String postProbe() {
      return "post-ok";
    }

    @PutMapping(PROBE_PATH)
    String putProbe() {
      return "put-ok";
    }

    @PatchMapping(PROBE_PATH)
    String patchProbe() {
      return "patch-ok";
    }

    @DeleteMapping(PROBE_PATH)
    String deleteProbe() {
      return "delete-ok";
    }

    @RequestMapping(value = PROBE_PATH, method = RequestMethod.HEAD)
    String headProbe() {
      return "head-ok";
    }

    @RequestMapping(value = PROBE_PATH, method = RequestMethod.OPTIONS)
    String optionsProbe() {
      return "options-ok";
    }

    /** Anticipated PR 3 review-endpoint shape, used by the exemption test. */
    @PostMapping(SEED_REVIEW_PATH)
    String postSeedReview() {
      return "seed-review-ok";
    }

    /** Stands in for Vaadin's own non-REST endpoints. */
    @PostMapping("/vaadin-uidl")
    String postUidl() {
      return "uidl-ok";
    }
  }
}
