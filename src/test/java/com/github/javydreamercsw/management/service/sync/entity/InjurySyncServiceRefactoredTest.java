package com.github.javydreamercsw.management.service.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.ai.notion.InjuryPage;
import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.test.BaseTest;
import com.github.javydreamercsw.base.util.EnvironmentVariableUtil;
import com.github.javydreamercsw.management.config.NotionSyncProperties;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.sync.SyncHealthMonitor;
import com.github.javydreamercsw.management.service.sync.SyncProgressTracker;
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Injury Sync Unit Tests")
class InjurySyncServiceRefactoredTest extends BaseTest {

  @Mock private ObjectMapper objectMapper;
  @Mock private NotionHandler notionHandler;
  @Mock private NotionSyncProperties syncProperties;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private InjuryTypeRepository injuryTypeRepository;
  @Mock private SyncProgressTracker progressTracker;
  @Mock private SyncHealthMonitor healthMonitor;

  private InjurySyncService injurySyncService;
  private ObjectMapper realObjectMapper;

  @BeforeEach
  void setUp() {
    injurySyncService = new InjurySyncService(objectMapper, syncProperties);
    setField(injurySyncService, "notionHandler", notionHandler);
    setField(injurySyncService, "progressTracker", progressTracker);
    setField(injurySyncService, "healthMonitor", healthMonitor);
    setField(injurySyncService, "injuryTypeService", injuryTypeService);
    setField(injurySyncService, "injuryTypeRepository", injuryTypeRepository);

    realObjectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("Should sync injury types from Notion sample data successfully")
  void shouldSyncInjuryTypesFromNotionSampleDataSuccessfully() throws Exception {
    try (MockedStatic<EnvironmentVariableUtil> mockedUtil =
        mockStatic(EnvironmentVariableUtil.class)) {
      mockedUtil.when(EnvironmentVariableUtil::isNotionTokenAvailable).thenReturn(true);

      List<InjuryPage> sampleInjuries = loadSampleInjuries();
      when(syncProperties.isEntityEnabled("injuries")).thenReturn(true);
      when(notionHandler.loadAllInjuries()).thenReturn(sampleInjuries);
      when(injuryTypeRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
      when(injuryTypeService.createInjuryType(
              anyString(), anyInt(), anyInt(), anyInt(), anyString()))
          .thenAnswer(
              invocation -> {
                InjuryType injuryType = new InjuryType();
                injuryType.setId(1L);
                injuryType.setInjuryName(invocation.getArgument(0));
                injuryType.setHealthEffect(invocation.getArgument(1));
                injuryType.setStaminaEffect(invocation.getArgument(2));
                injuryType.setCardEffect(invocation.getArgument(3));
                injuryType.setSpecialEffects(invocation.getArgument(4));
                return injuryType;
              });
      when(injuryTypeService.updateInjuryType(any(InjuryType.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      BaseSyncService.SyncResult result = injurySyncService.syncInjuryTypes("test-operation-id");

      assertThat(result).isNotNull();
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getEntityType()).isEqualTo("Injuries");
      assertThat(result.getSyncedCount()).isEqualTo(3);
      assertThat(result.getErrorCount()).isEqualTo(0);

      verify(notionHandler).loadAllInjuries();
      verify(injuryTypeService, times(3))
          .createInjuryType(anyString(), anyInt(), anyInt(), anyInt(), anyString());
      verify(injuryTypeService, times(3)).updateInjuryType(any(InjuryType.class));
    }
  }

  private List<InjuryPage> loadSampleInjuries() throws Exception {
    File samplesDir = new File("src/test/resources/notion-samples");
    InjuryPage injury1 =
        realObjectMapper.readValue(new File(samplesDir, "real-injury-1.json"), InjuryPage.class);
    InjuryPage injury2 =
        realObjectMapper.readValue(new File(samplesDir, "real-injury-2.json"), InjuryPage.class);
    InjuryPage injury3 =
        realObjectMapper.readValue(new File(samplesDir, "real-injury-3.json"), InjuryPage.class);
    return Arrays.asList(injury1, injury2, injury3);
  }
}
