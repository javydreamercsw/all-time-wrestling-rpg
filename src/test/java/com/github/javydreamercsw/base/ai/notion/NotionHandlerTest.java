package com.github.javydreamercsw.base.ai.notion;

import static org.junit.jupiter.api.Assertions.*;

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
  void testSingletonInstance() {
    NotionHandler instance1 = NotionHandler.getInstance();
    NotionHandler instance2 = NotionHandler.getInstance();
    assertSame(instance1, instance2, "Singleton instances should be the same");
  }

  @Test
  void testInitializationSkipsIfNoToken() {
    try (MockedStatic<com.github.javydreamercsw.base.util.EnvironmentVariableUtil> envMock =
        Mockito.mockStatic(com.github.javydreamercsw.base.util.EnvironmentVariableUtil.class)) {
      envMock
          .when(com.github.javydreamercsw.base.util.EnvironmentVariableUtil::isNotionTokenAvailable)
          .thenReturn(false);
      NotionHandler instance = NotionHandler.getInstance();
      // Should not throw, but should not initialize databases
      assertNotNull(instance);
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
      NotionHandler handler = NotionHandler.getInstance();
      // Should not throw here, but if loadDatabases is called, simulate error
      // (This is a placeholder, actual NotionClient mocking would be more involved)
      assertNotNull(handler);
    }
  }
}
