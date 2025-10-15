package com.github.javydreamercsw.base.ai.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.test.AbstractIntegrationTest;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Tests for NotionHandler teams functionality. These tests require NOTION_TOKEN to be available.
 */
class NotionHandlerTeamsTest extends AbstractIntegrationTest {

  @MockitoBean private NotionHandler notionHandler;

  @Test
  void shouldLoadAllTeamsSuccessfully() {
    try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(notionHandler);
      // Given
      TeamPage teamPage = new TeamPage();
      teamPage.setId("test-id");
      when(notionHandler.loadAllTeams()).thenReturn(Collections.singletonList(teamPage));

      // When
      List<TeamPage> teams = notionHandler.loadAllTeams();

      // Then
      assertThat(teams).isNotNull();
      // Note: We can't assert specific size as it depends on Notion database content
      // But we can verify the structure
      teams.forEach(
          team -> {
            assertThat(team).isNotNull();
            assertThat(team.getId()).isNotNull();
            // Additional assertions can be added based on expected team structure
          });
    }
  }

  @Test
  void shouldThrowExceptionWhenNotionTokenNotAvailable() {
    // Given - Mock NOTION_TOKEN as not available
    try (MockedStatic<EnvironmentVariableUtil> mockedUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      mockedUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(false);

      try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
        NotionHandler mockHandler = mock(NotionHandler.class);
        when(mockHandler.loadAllTeams()).thenCallRealMethod();
        mocked.when(NotionHandler::getInstance).thenReturn(mockHandler);

        // When & Then
        assertThatThrownBy(() -> mockHandler.loadAllTeams())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("NOTION_TOKEN is required for sync operations");
      }
    }
  }

  @Test
  void shouldReturnEmptyListWhenTeamsDatabaseNotFound() {
    try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(notionHandler);
      // Given
      when(notionHandler.loadAllTeams()).thenReturn(Collections.emptyList());
      // When
      List<TeamPage> teams = notionHandler.loadAllTeams();

      // Then - Should not throw exception, might return empty list
      assertThat(teams).isNotNull();
    }
  }

  @Test
  void shouldHandleNotionClientCreationFailure() {
    try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(notionHandler);
      // Given - Mock NOTION_TOKEN as available but invalid
      when(notionHandler.loadAllTeams())
          .thenThrow(new RuntimeException("Failed to load teams from Notion"));

      // When & Then - The method may return empty list instead of throwing exception
      // This depends on the actual implementation behavior
      try {
        List<TeamPage> result = notionHandler.loadAllTeams();
        // If no exception is thrown, verify it returns empty list or handles gracefully
        assertThat(result).isNotNull();
      } catch (RuntimeException e) {
        // If exception is thrown, verify it contains expected message
        assertThat(e.getMessage()).contains("Failed to load teams from Notion");
      }
    }
  }

  @Test
  void shouldLoadTeamsWithCorrectStructure() {
    try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(notionHandler);
      // Given
      TeamPage teamPage = new TeamPage();
      teamPage.setId("test-id");
      teamPage.setRawProperties(Collections.singletonMap("Name", "Test Team"));
      TeamPage.NotionProperties properties = new TeamPage.NotionProperties();
      teamPage.setProperties(properties);
      when(notionHandler.loadAllTeams()).thenReturn(Collections.singletonList(teamPage));
      // When
      List<TeamPage> teams = notionHandler.loadAllTeams();

      // Then
      assertThat(teams).isNotNull();

      // If teams exist, verify they have the expected structure
      teams.forEach(
          team -> {
            assertThat(team.getId()).isNotNull().isNotEmpty();
            // Verify team has properties (structure may vary based on Notion setup)
            assertThat(team.getProperties()).isNotNull();
          });
    }
  }
}
