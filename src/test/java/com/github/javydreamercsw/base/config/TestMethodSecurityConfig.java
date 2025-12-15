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

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Test configuration for method security. Provides a simple in-memory user for tests to avoid
 * authentication errors.
 */
@TestConfiguration
@Profile("test")
@EnableMethodSecurity(prePostEnabled = false) // Disable method security in tests
public class TestMethodSecurityConfig {

  @Bean
  @Primary
  public UserDetailsService testUserDetailsService() {
    // Create a test user with all roles
    UserDetails user =
        User.withDefaultPasswordEncoder()
            .username("testuser")
            .password("password")
            .roles("ADMIN", "BOOKER", "PLAYER", "VIEWER")
            .build();

    return new InMemoryUserDetailsManager(user);
  }
}
