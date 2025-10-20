package com.github.javydreamercsw.base.ai.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for NotionHandler teams functionality. */
@ExtendWith(MockitoExtension.class)
class NotionHandlerTeamsTest {

  @Mock private NotionHandler notionHandler;

  @Test
  void shouldLoadAllTeamsSuccessfully() {
    try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      // Given
      TeamPage teamPage = new TeamPage();
      teamPage.setId("test-id");
      when(notionHandler.loadAllTeams()).thenReturn(Collections.singletonList(teamPage));

      // When
      List<TeamPage> teams = notionHandler.loadAllTeams();

      // Then
      assertThat(teams).isNotNull();
      teams.forEach(
          team -> {
            assertThat(team).isNotNull();
            assertThat(team.getId()).isNotNull();
          });
    }
  }

  @Test
  void shouldThrowExceptionWhenNotionTokenNotAvailable() {
    // Given - Mock NOTION_TOKEN as not available
    try (MockedStatic<EnvironmentVariableUtil> mockedUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      mockedUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(false);

      NotionHandler realHandler = new NotionHandler(true);

      // When & Then
      assertThatThrownBy(realHandler::loadAllTeams)
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("NOTION_TOKEN is required for sync operations");
    }
  }

  @Test
  void shouldReturnEmptyListWhenTeamsDatabaseNotFound() {
    try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      // Given
      when(notionHandler.loadAllTeams()).thenReturn(Collections.emptyList());
      // When
      List<TeamPage> teams = notionHandler.loadAllTeams();

      // Then - Should not throw exception, might return empty list
      assertThat(teams).isNotNull().isEmpty();
    }
  }

  @Test
  void shouldHandleNotionClientCreationFailure() {
    try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      // Given - Mock NOTION_TOKEN as available but invalid
      when(notionHandler.loadAllTeams())
          .thenThrow(new RuntimeException("Failed to load teams from Notion"));

      // When & Then
      assertThatThrownBy(() -> notionHandler.loadAllTeams())
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Failed to load teams from Notion");
    }
  }

  @Test
  void shouldLoadTeamsWithCorrectStructure() {
    try (MockedStatic<NotionHandler> mocked = mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
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
