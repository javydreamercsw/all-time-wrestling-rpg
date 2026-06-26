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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class UrlUtil {

  // Cached during the first real HTTP request so WebSocket callers get the right context path.
  private static volatile String cachedNetworkUrl = null;

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
   *
   * <p>When called outside a Vaadin HTTP request (e.g. from a WebSocket click listener),
   * VaadinServletRequest.getCurrent() returns null. In that case the value computed during the most
   * recent real HTTP request is reused, preserving the correct context path. If no HTTP request has
   * been seen yet the method falls back to a best-effort LAN IP guess.
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
      String result;
      if (forwardedHost != null && !forwardedHost.isBlank()) {
        String scheme = forwardedProto != null ? forwardedProto : "https";
        String contextPath = request.getHttpServletRequest().getContextPath();
        result = scheme + "://" + forwardedHost + contextPath;
      } else {
        // Real HTTP request — swap localhost for LAN IP and preserve context path.
        String base = getBaseUrl();
        String lanIp = getLanIpAddress();
        result =
            lanIp != null ? base.replace("localhost", lanIp).replace("127.0.0.1", lanIp) : base;
      }
      cachedNetworkUrl = result;
      return result;
    }
    // No HTTP request context (WebSocket/push thread) — use cached value to keep context path.
    if (cachedNetworkUrl != null) {
      return cachedNetworkUrl;
    }
    // Last resort: no cache yet, build from LAN IP (context path unknown).
    String lanIp = getLanIpAddress();
    if (lanIp != null) {
      return "http://" + lanIp + ":8080";
    }
    return "http://localhost:8080";
  }

  /** Returns the first non-loopback site-local IPv4 address, or null if none found. */
  static String getLanIpAddress() {
    try {
      for (NetworkInterface iface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
        if (!iface.isUp() || iface.isLoopback() || iface.isVirtual() || iface.isPointToPoint()) {
          continue;
        }
        for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
          if (!addr.isLoopbackAddress()
              && addr.isSiteLocalAddress()
              && addr.getAddress().length == 4) { // IPv4 only
            return addr.getHostAddress();
          }
        }
      }
    } catch (Exception e) {
      log.warn("Could not determine LAN IP address", e);
    }
    return null;
  }
}
