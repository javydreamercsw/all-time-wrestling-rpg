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
package com.github.javydreamercsw.management.test;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@Import({
  com.github.javydreamercsw.base.config.TestSecurityConfig.class,
  com.github.javydreamercsw.management.config.TestNotionConfiguration.class
})
@ActiveProfiles("test")
@DirtiesContext
public abstract class AbstractMockUserIntegrationTest extends AbstractIntegrationTest {

  @Autowired private AccountRepository accountRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @BeforeEach
  public void defaultLogin() {
    // Provide a default admin context if none was provided by annotations
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      accountRepository.findByUsername("admin").ifPresent(this::login);
    }
  }

  protected void login(Account account) {
    java.util.List<Wrestler> wrestlers = wrestlerRepository.findByAccount(account);
    Wrestler wrestler = wrestlers.isEmpty() ? null : wrestlers.get(0);
    CustomUserDetails principal = new CustomUserDetails(account, wrestler);

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    for (Role role : account.getRoles()) {
      String roleName = role.getName().name();
      authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
      authorities.add(new SimpleGrantedAuthority(roleName));
    }

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, account.getPassword(), authorities);

    SecurityContextHolder.getContext().setAuthentication(authentication);
    TestSecurityContextHolder.setAuthentication(authentication);
  }
}
