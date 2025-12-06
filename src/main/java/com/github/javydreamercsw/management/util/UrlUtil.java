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
import org.springframework.web.util.UriComponentsBuilder;

public final class UrlUtil {

  private UrlUtil() {
    // Utility class
  }

  public static String getBaseUrl() {
    VaadinServletRequest request = VaadinServletRequest.getCurrent();
    if (request != null) {
      return UriComponentsBuilder.fromUriString(
              request.getHttpServletRequest().getRequestURL().toString())
          .replacePath(request.getHttpServletRequest().getContextPath())
          .build()
          .toUriString();
    }
    // Fallback for testing or background threads
    return "http://localhost:8080";
  }
}
