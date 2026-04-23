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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private ApplicationContext context;

  @Test
  void testMainSecurityFilterChainBeanExists() {
    // Check for either regular or test filter chain
    boolean hasFilterChain =
        context.containsBean("vaadinSecurityFilterChain")
            || context.containsBean("testSecurityFilterChain");
    assertThat(hasFilterChain).isTrue();

    SecurityFilterChain filterChain = context.getBean(SecurityFilterChain.class);
    assertThat(filterChain).isNotNull();
  }
}
