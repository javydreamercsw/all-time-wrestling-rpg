package com.github.javydreamercsw.management.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javydreamercsw.Application;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.service.deck.DeckService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(classes = {Application.class})
@ActiveProfiles("test")
@Transactional
public class DataMigrationServiceTest {

  @Autowired private DataMigrationService dataMigrationService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private CardService cardService;
  @Autowired private CardSetService cardSetService;
  @Autowired private DeckService deckService;

  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private DeckRepository deckRepository;
  @PersistenceContext private EntityManager entityManager;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.registerModule(new JavaTimeModule());
    entityManager.flush();
    entityManager.clear();
    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE deck_card").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE deck").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE card").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE title_reign").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE team").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE wrestler").executeUpdate();
    entityManager.createNativeQuery("TRUNCATE TABLE card_set").executeUpdate();
    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();

    // Create a default CardSet for tests that create cards
    cardSetService.createCardSet("Default Test Set", "DTS");
  }

  @Test
  void testExportDataAsJson() throws IOException {
    // Create some test data
    Wrestler wrestler = wrestlerService.createWrestler("Test Wrestler", true, "Description");
    cardSetService.createCardSet("Test Set", "TS");
    cardService.createCard("Test Card");
    deckService.createDeck(wrestler);

    byte[] exportedData = dataMigrationService.exportData(DataMigrationService.DataFormat.JSON);
    assertNotNull(exportedData);
    assertTrue(exportedData.length > 0);

    // Verify exported data structure
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(exportedData))) {
      Map<String, byte[]> zipEntryContents = new HashMap<>();
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        String fileName = zipEntry.getName();
        ByteArrayOutputStream entryBaos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) > 0) {
          entryBaos.write(buffer, 0, len);
        }
        zipEntryContents.put(
            fileName.substring(0, fileName.lastIndexOf('.')), entryBaos.toByteArray());
        zipEntry = zis.getNextEntry();
      }

      assertTrue(zipEntryContents.containsKey("wrestlerRepository"));
      assertTrue(zipEntryContents.containsKey("cardSetRepository"));
      assertTrue(zipEntryContents.containsKey("cardRepository"));
      assertTrue(zipEntryContents.containsKey("deckRepository"));

      // Verify wrestler data
      List<Wrestler> wrestlers =
          objectMapper.readValue(
              new String(zipEntryContents.get("wrestlerRepository"), StandardCharsets.UTF_8),
              objectMapper.getTypeFactory().constructCollectionType(List.class, Wrestler.class));
      assertEquals(1, wrestlers.size());
      assertEquals("Test Wrestler", wrestlers.get(0).getName());

      // Verify cardSet data
      List<CardSet> cardSets =
          objectMapper.readValue(
              new String(zipEntryContents.get("cardSetRepository"), StandardCharsets.UTF_8),
              objectMapper.getTypeFactory().constructCollectionType(List.class, CardSet.class));
      assertEquals(2, cardSets.size()); // Default Test Set + Test Set
      assertTrue(cardSets.stream().anyMatch(cs -> cs.getName().equals("Test Set")));

      // Verify card data
      List<Card> cards =
          objectMapper.readValue(
              new String(zipEntryContents.get("cardRepository"), StandardCharsets.UTF_8),
              objectMapper.getTypeFactory().constructCollectionType(List.class, Card.class));
      assertEquals(1, cards.size());
      assertEquals("Test Card", cards.get(0).getName());

      // Verify deck data
      List<Deck> decks =
          objectMapper.readValue(
              new String(zipEntryContents.get("deckRepository"), StandardCharsets.UTF_8),
              objectMapper.getTypeFactory().constructCollectionType(List.class, Deck.class));
      assertEquals(1, decks.size());
      assertEquals(wrestler.getId(), decks.get(0).getWrestler().getId());
    }
  }

  // @Test
  // void testImportDataAsJson() throws IOException {
  //  // Create some initial data
  //  Wrestler wrestler = wrestlerService.createWrestler("Initial Wrestler", true, "Description");
  //  cardSetService.createCardSet("Initial Set", "IS");
  //  cardService.createCard("Initial Card");
  //  deckService.createDeck(wrestler);
  //
  //  // Export the initial data
  //  byte[] exportedData = dataMigrationService.exportData(DataMigrationService.DataFormat.JSON);
  //
  //  // Clear the database
  //  entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
  //  entityManager.createNativeQuery("TRUNCATE TABLE deck_card").executeUpdate();
  //  entityManager.createNativeQuery("TRUNCATE TABLE deck").executeUpdate();
  //  entityManager.createNativeQuery("TRUNCATE TABLE card").executeUpdate();
  //  entityManager.createNativeQuery("TRUNCATE TABLE title_reign").executeUpdate();
  //  entityManager.createNativeQuery("TRUNCATE TABLE team").executeUpdate();
  //  entityManager.createNativeQuery("TRUNCATE TABLE wrestler").executeUpdate();
  //  entityManager.createNativeQuery("TRUNCATE TABLE card_set").executeUpdate();
  //  entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
  //
  //  // Verify database is empty
  //  assertEquals(0, wrestlerRepository.count());
  //  assertEquals(0, cardRepository.count());
  //  assertEquals(0, cardSetRepository.count());
  //  assertEquals(0, deckRepository.count());
  //
  //  // Import the data
  //  dataMigrationService.importData(DataMigrationService.DataFormat.JSON, exportedData);
  //
  //  // Ensure entity manager is flushed and cleared to reflect imported data
  //  entityManager.flush();
  //  entityManager.clear();
  //
  //  // Verify data is restored
  //  assertEquals(1, wrestlerRepository.count());
  //  assertEquals(1, cardRepository.count());
  //  assertEquals(1, cardSetRepository.count());
  //  assertEquals(1, deckRepository.count());
  //
  //  Optional<Wrestler> importedWrestler = wrestlerRepository.findByName("Initial Wrestler");
  //  assertTrue(importedWrestler.isPresent());
  //  assertEquals("Initial Wrestler", importedWrestler.get().getName());
  // }
}
