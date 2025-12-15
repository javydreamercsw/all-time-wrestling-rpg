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

import com.github.javydreamercsw.management.DataInitializer;
import java.util.Collection;
import java.util.List;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class RunAsManagerImpl implements RunAsManager {

  @Override
  public Authentication buildRunAs(
      Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
    if (object instanceof DataInitializer) {
      List<GrantedAuthority> newAuthorities = List.of(new SimpleGrantedAuthority("ADMIN"));
      return new UsernamePasswordAuthenticationToken(
          authentication.getPrincipal(), authentication.getCredentials(), newAuthorities);
    }
    return null;
  }

  @Override
  public boolean supports(ConfigAttribute attribute) {
    return false;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return true;
  }
}
