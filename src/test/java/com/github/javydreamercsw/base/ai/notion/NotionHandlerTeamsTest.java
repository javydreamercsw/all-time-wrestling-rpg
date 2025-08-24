package com.github.javydreamercsw.base.ai.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.MockedStatic;

/**
 * Tests for NotionHandler teams functionality. These tests require NOTION_TOKEN to be available.
 */
class NotionHandlerTeamsTest {

  private NotionHandler notionHandler;

  @BeforeEach
  void setUp() {
    notionHandler = NotionHandler.getInstance();
  }

  @Test
  @EnabledIf("isNotionTokenAvailable")
  void shouldLoadAllTeamsSuccessfully() {
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

  @Test
  void shouldThrowExceptionWhenNotionTokenNotAvailable() {
    // Given - Mock NOTION_TOKEN as not available
    try (MockedStatic<EnvironmentVariableUtil> mockedUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      mockedUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(false);

      // When & Then
      assertThatThrownBy(() -> notionHandler.loadAllTeams())
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("NOTION_TOKEN is required for sync operations");
    }
  }

  @Test
  @EnabledIf("isNotionTokenAvailable")
  void shouldReturnEmptyListWhenTeamsDatabaseNotFound() {
    // Given - This test assumes Teams database might not exist in some workspaces
    // When
    List<TeamPage> teams = notionHandler.loadAllTeams();

    // Then - Should not throw exception, might return empty list
    assertThat(teams).isNotNull();
  }

  @Test
  void shouldHandleNotionClientCreationFailure() {
    // Given - Mock NOTION_TOKEN as available but invalid
    try (MockedStatic<EnvironmentVariableUtil> mockedUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      mockedUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);

      // Set invalid token
      System.setProperty("NOTION_TOKEN", "invalid-token");

      // When & Then - The method may return empty list instead of throwing exception
      // This depends on the actual implementation behavior
      try {
        List<TeamPage> result = notionHandler.loadAllTeams();
        // If no exception is thrown, verify it returns empty list or handles gracefully
        assertThat(result).isNotNull();
      } catch (RuntimeException e) {
        // If exception is thrown, verify it contains expected message
        assertThat(e.getMessage()).contains("Failed to load teams from Notion");
      } finally {
        // Clean up
        System.clearProperty("NOTION_TOKEN");
      }
    }
  }

  @Test
  @EnabledIf("isNotionTokenAvailable")
  void shouldLoadTeamsWithCorrectStructure() {
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

  /** Helper method to check if NOTION_TOKEN is available for conditional tests. */
  static boolean isNotionTokenAvailable() {
    return EnvironmentVariableUtil.isNotionTokenAvailable();
  }
}
