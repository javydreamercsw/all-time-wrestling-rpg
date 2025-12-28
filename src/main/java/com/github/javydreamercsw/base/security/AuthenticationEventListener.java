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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationEventListener
    implements ApplicationListener<AbstractAuthenticationEvent> {

  private final CustomUserDetailsService userDetailsService;

  @Override
  public void onApplicationEvent(AbstractAuthenticationEvent event) {
    if (event instanceof AuthenticationSuccessEvent) {
      handleSuccessfulAuthentication((AuthenticationSuccessEvent) event);
    } else if (event instanceof AuthenticationFailureBadCredentialsEvent) {
      handleFailedAuthentication((AuthenticationFailureBadCredentialsEvent) event);
    }
    // Other failure events can be handled here if needed (e.g., locked, disabled, etc.)
  }

  private void handleSuccessfulAuthentication(AuthenticationSuccessEvent event) {
    if (event.getAuthentication().getPrincipal() instanceof CustomUserDetails userDetails) {
      String username = userDetails.getUsername();
      userDetailsService.recordSuccessfulLogin(username);
      log.debug("Successful login for user: {}", username);
    }
  }

  private void handleFailedAuthentication(AuthenticationFailureBadCredentialsEvent event) {
    String username = (String) event.getAuthentication().getPrincipal();
    log.warn("Failed login attempt for user: {}", username);
  }
}
