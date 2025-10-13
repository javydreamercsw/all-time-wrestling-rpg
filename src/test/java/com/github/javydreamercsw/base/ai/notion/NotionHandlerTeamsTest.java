package com.github.javydreamercsw.base.ai.notion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import notion.api.v1.NotionClient;
import notion.api.v1.model.databases.QueryResults;
import notion.api.v1.model.pages.Page;
import notion.api.v1.model.pages.PageParent;
import notion.api.v1.model.pages.PageProperty;
import notion.api.v1.request.databases.QueryDatabaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotionHandlerTeamsTest {

  private NotionHandler notionHandler;
  private NotionClient notionClient;

  @BeforeEach
  void setUp() {
    notionHandler = spy(new NotionHandler(true));
    notionClient = mock(NotionClient.class);
    doReturn(notionClient).when(notionHandler).createNotionClient();
    notionHandler.databaseMap.put("Teams", "test_db_id");
  }

  @Test
  void testLoadAllTeams() {
    try (MockedStatic<EnvironmentVariableUtil> envMock =
        mockStatic(EnvironmentVariableUtil.class)) {
      envMock.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);
      // Given
      Page page = mock(Page.class);
      Map<String, PageProperty> properties = new HashMap<>();
      PageProperty nameProperty = new PageProperty();
      List<PageProperty.RichText> title = new ArrayList<>();
      PageProperty.RichText richText = new PageProperty.RichText();
      richText.setPlainText("Test Team");
      title.add(richText);
      nameProperty.setTitle(title);
      properties.put("Name", nameProperty);

      when(page.getProperties()).thenReturn(properties);
      when(page.getId()).thenReturn("test-id");

      // Mock parent
      PageParent parent = mock(PageParent.class);
      when(parent.getDatabaseId()).thenReturn("test_db_id");
      when(page.getParent()).thenReturn(parent);

      QueryResults queryResults = mock(QueryResults.class);
      when(queryResults.getResults()).thenReturn(Collections.singletonList(page));

      when(notionClient.queryDatabase(new QueryDatabaseRequest("test_db_id")))
          .thenReturn(queryResults);

      when(notionClient.retrievePage(page.getId(), Collections.emptyList())).thenReturn(page);

      // When
      List<TeamPage> teams = notionHandler.loadAllTeams();

      // Then
      assertThat(teams).isNotNull().hasSize(1);
      TeamPage resultTeam = teams.get(0);
      assertThat(resultTeam.getId()).isEqualTo("test-id");
      assertThat(resultTeam.getRawProperties()).isNotNull();
      assertThat(resultTeam.getRawProperties().get("Name")).isEqualTo("Test Team");
    }
  }
}
