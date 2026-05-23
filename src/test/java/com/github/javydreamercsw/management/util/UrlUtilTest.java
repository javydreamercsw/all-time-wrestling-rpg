/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class UrlUtilTest {

  @Test
  void getLanIpAddressReturnsNonLocalhostAddress() {
    String ip = UrlUtil.getLanIpAddress();
    // May be null in CI with no network interfaces, but must not be a loopback if present
    if (ip != null) {
      assertNotEquals("127.0.0.1", ip);
      assertNotEquals("::1", ip);
      assertFalse(ip.isBlank());
    }
  }

  @Test
  void getNetworkUrlWithoutRequestContextDoesNotReturnLocalhostWhenLanAvailable() {
    // No VaadinServletRequest is available in this unit test context
    String url = UrlUtil.getNetworkUrl();
    assertNotNull(url);
    assertFalse(url.isBlank());
    // If a LAN IP was found the URL must not say localhost
    String lanIp = UrlUtil.getLanIpAddress();
    if (lanIp != null) {
      assertFalse(url.contains("localhost"), "Expected LAN IP in URL but got: " + url);
      assertFalse(url.contains("127.0.0.1"), "Expected LAN IP in URL but got: " + url);
    }
  }
}
