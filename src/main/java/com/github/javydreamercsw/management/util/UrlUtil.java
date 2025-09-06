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
