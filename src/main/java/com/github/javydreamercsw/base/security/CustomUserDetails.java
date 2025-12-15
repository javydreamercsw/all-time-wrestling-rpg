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

import com.github.javydreamercsw.base.domain.account.Account;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Custom UserDetails implementation that wraps our Account entity. */
@Getter
public class CustomUserDetails implements UserDetails {

  private final Account account;

  public CustomUserDetails(Account account) {
    this.account = account;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return account.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
        .collect(Collectors.toSet());
  }

  @Override
  public String getPassword() {
    return account.getPassword();
  }

  @Override
  public String getUsername() {
    return account.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    return account.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    // Check if account is locked and if lock has expired
    if (!account.isAccountNonLocked()) {
      return account.isLockExpired();
    }
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return account.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return account.isEnabled();
  }

  /**
   * Get the email of the user.
   *
   * @return the email
   */
  public String getEmail() {
    return account.getEmail();
  }

  /**
   * Get the account ID.
   *
   * @return the account ID
   */
  public Long getId() {
    return account.getId();
  }
}
