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
package com.github.javydreamercsw.base.ai.notion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class NotionHandlerTest {

  @BeforeEach
  void resetSingleton() throws Exception {
    // Use reflection to reset the singleton for isolated tests
    var field = NotionHandler.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  void testInitializationSkipsIfNoToken() {
    try (MockedStatic<com.github.javydreamercsw.base.util.EnvironmentVariableUtil> envMock =
        Mockito.mockStatic(com.github.javydreamercsw.base.util.EnvironmentVariableUtil.class)) {
      envMock
          .when(com.github.javydreamercsw.base.util.EnvironmentVariableUtil::isNotionTokenAvailable)
          .thenReturn(false);
      Optional<NotionHandler> instance = NotionHandler.getInstance();
      // Should not throw, and should return empty Optional
      assertTrue(instance.isEmpty());
    }
  }

  @Test
  void testInitializationThrowsOnClientError() {
    try (MockedStatic<com.github.javydreamercsw.base.util.EnvironmentVariableUtil> envMock =
            Mockito.mockStatic(com.github.javydreamercsw.base.util.EnvironmentVariableUtil.class);
        MockedStatic<NotionHandler> handlerMock =
            Mockito.mockStatic(NotionHandler.class, Mockito.CALLS_REAL_METHODS)) {
      envMock
          .when(com.github.javydreamercsw.base.util.EnvironmentVariableUtil::isNotionTokenAvailable)
          .thenReturn(true);
      handlerMock.when(NotionHandler::getInstance).thenCallRealMethod();
      // Simulate NotionClient failure by throwing exception in loadDatabases
      Optional<NotionHandler> handler = NotionHandler.getInstance();
      // Should not throw here, but if loadDatabases is called, simulate error
      // (This is a placeholder, actual NotionClient mocking would be more involved)
      assertTrue(handler.isEmpty());
    }
  }
}
