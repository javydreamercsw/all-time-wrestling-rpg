package com.github.javydreamercsw.management.service.sync.entity.notion.outgoing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.notion.NotionHandler;
import com.github.javydreamercsw.base.ai.notion.WrestlerPage;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.service.sync.entity.notion.WrestlerNotionSyncService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import notion.api.v1.model.pages.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class WrestlerNotionSyncServiceIT extends ManagementIntegrationTest {

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private FactionRepository factionRepository;
  @Autowired private WrestlerNotionSyncService wrestlerNotionSyncService;

  @MockBean private NotionHandler notionHandler;

  @Captor private ArgumentCaptor<WrestlerPage> wrestlerPageCaptor;

  @BeforeEach
  public void setup() {
    clearAllRepositories();
  }

  @Test
  void testSyncToNotion() {
    try (MockedStatic<NotionHandler> mocked = Mockito.mockStatic(NotionHandler.class)) {
      mocked.when(NotionHandler::getInstance).thenReturn(Optional.of(notionHandler));
      
      String newPageId = UUID.randomUUID().toString();
      Page newPage = new Page();
      newPage.setId(newPageId);

      when(notionHandler.createWrestlerPage(any(WrestlerPage.class))).thenReturn(newPage);
      when(notionHandler.updateWrestlerPage(any(WrestlerPage.class))).thenReturn(newPage);

      // Create a new faction
      Faction faction = new Faction();
      faction.setName("Test Faction " + UUID.randomUUID());
      factionRepository.save(faction);

      // Create a new wrestler
      Wrestler wrestler = new Wrestler();
      wrestler.setName("Test Wrestler " + UUID.randomUUID());
      wrestler.setStartingStamina(16);
      wrestler.setFans(1000L);
      wrestler.setBumps(1);
      wrestler.setGender(Gender.MALE);
      wrestler.setLowStamina(2);
      wrestler.setStartingHealth(15);
      wrestler.setLowHealth(4);
      wrestler.setDeckSize(15);
      wrestler.setTier(WrestlerTier.MIDCARDER);
      wrestler.setCreationDate(Instant.now());
      wrestler.setFaction(faction);
      wrestlerRepository.save(wrestler);

      // Sync to Notion for the first time
      wrestlerNotionSyncService.syncToNotion("test-op-1");

      // Verify that the externalId and lastSync fields are updated
      assertNotNull(wrestler.getId());
      Wrestler updatedWrestler = wrestlerRepository.findById(wrestler.getId()).get();
      assertNotNull(updatedWrestler.getExternalId());
      assertNotNull(updatedWrestler.getLastSync());
      assertEquals(newPageId, updatedWrestler.getExternalId());

      // Verify properties sent to Notion
      Mockito.verify(notionHandler).createWrestlerPage(wrestlerPageCaptor.capture());
      WrestlerPage capturedPage = wrestlerPageCaptor.getValue();
      assertEquals(wrestler.getName(), capturedPage.getName());
      assertEquals(wrestler.getFans(), capturedPage.getFans());

      // Sync to Notion again
      updatedWrestler.setName("Test Wrestler Updated " + UUID.randomUUID());
      wrestlerRepository.save(updatedWrestler);
      wrestlerNotionSyncService.syncToNotion("test-op-2");
      Wrestler updatedWrestler2 = wrestlerRepository.findById(wrestler.getId()).get();
      assertTrue(updatedWrestler2.getLastSync().isAfter(updatedWrestler.getLastSync()));

      // Verify updated name sent to Notion
      Mockito.verify(notionHandler).updateWrestlerPage(wrestlerPageCaptor.capture());
      capturedPage = wrestlerPageCaptor.getValue();
      assertEquals(updatedWrestler2.getName(), capturedPage.getName());
    }
  }
}
