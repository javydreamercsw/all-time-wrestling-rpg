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
package com.github.javydreamercsw.management.util;

import com.vaadin.flow.server.VaadinServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.util.UriComponentsBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UrlUtil {

  public static String getBaseUrl() {
    VaadinServletRequest request = VaadinServletRequest.getCurrent();
    if (request != null) {
      return UriComponentsBuilder.fromUriString(
              request.getHttpServletRequest().getRequestURL().toString())
          .replacePath(request.getHttpServletRequest().getContextPath())
          .build()
          .toUriString();
    }
    return "http://localhost:8080";
  }

  /**
   * Like getBaseUrl() but replaces localhost/127.0.0.1 with the machine's LAN IP so QR codes are
   * scannable from phones on the same network.
   */
  public static String getNetworkUrl() {
    String override = System.getenv("QR_BASE_URL");
    if (override != null && !override.isBlank()) {
      return override;
    }
    VaadinServletRequest request = VaadinServletRequest.getCurrent();
    if (request != null) {
      String forwardedHost = request.getHttpServletRequest().getHeader("X-Forwarded-Host");
      String forwardedProto = request.getHttpServletRequest().getHeader("X-Forwarded-Proto");
      if (forwardedHost != null && !forwardedHost.isBlank()) {
        String scheme = forwardedProto != null ? forwardedProto : "https";
        String contextPath = request.getHttpServletRequest().getContextPath();
        return scheme + "://" + forwardedHost + contextPath;
      }
    }
    return getBaseUrl();
  }
}
