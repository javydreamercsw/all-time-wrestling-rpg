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

import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.Optional;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Configuration for security context in tests. */
@TestConfiguration
public class TestSecurityContextConfig {

  @Bean
  @Primary
  public AuthenticationContext testAuthenticationContext() {
    AuthenticationContext mock = Mockito.mock(AuthenticationContext.class);

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

    Mockito.when(mock.hasRole(Mockito.anyString()))
        .thenAnswer(
            inv -> {
              String role = inv.getArgument(0);
              Authentication auth = SecurityContextHolder.getContext().getAuthentication();
              if (auth == null) return false;
              return auth.getAuthorities().stream()
                  .anyMatch(
                      a ->
                          a.getAuthority().equals(role) || a.getAuthority().equals("ROLE_" + role));
            });

    return mock;
  }
}
