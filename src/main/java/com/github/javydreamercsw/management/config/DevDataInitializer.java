package com.github.javydreamercsw.management.config;

import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.faction.FactionRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Development data initializer that creates sample factions with members for testing and
 * development purposes. This ensures the UI has data to display when running the application
 * locally.
 */
@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements CommandLineRunner {

  private final FactionService factionService;
  private final FactionRepository factionRepository;
  private final WrestlerRepository wrestlerRepository;

  @Override
  @Transactional
  public void run(String... args) throws Exception {
    log.info("🚀 Initializing development data...");

    // Only initialize if no factions exist
    if (factionRepository.count() > 0) {
      log.info("✅ Development data already exists, skipping initialization");
      return;
    }

    try {
      createSampleWrestlersAndFactions();
      log.info("✅ Development data initialization completed successfully");
    } catch (Exception e) {
      log.error("❌ Failed to initialize development data", e);
    }
  }

  private void createSampleWrestlersAndFactions() {
    log.info("📝 Creating sample wrestlers and factions...");

    // Create wrestlers
    Wrestler tripleH = createWrestler("Triple H", 15, 4, 18, 4);
    Wrestler randyOrton = createWrestler("Randy Orton", 16, 2, 16, 2);
    Wrestler ric = createWrestler("Ric Flair", 14, 3, 17, 3);
    Wrestler batista = createWrestler("Batista", 18, 5, 20, 5);

    // Create Evolution faction
    Faction evolution = createFaction("Evolution", "A dominant faction in WWE", true);

    // Add members to Evolution
    factionService.addMemberToFaction(evolution.getId(), tripleH.getId());
    factionService.addMemberToFaction(evolution.getId(), randyOrton.getId());
    factionService.addMemberToFaction(evolution.getId(), ric.getId());
    factionService.addMemberToFaction(evolution.getId(), batista.getId());

    // Set Triple H as leader
    evolution.setLeader(tripleH);
    factionRepository.save(evolution);

    // Create a second faction for more test data
    Wrestler johnCena = createWrestler("John Cena", 17, 3, 19, 3);
    Wrestler theRock = createWrestler("The Rock", 16, 4, 18, 4);

    Faction legacyFaction =
        createFaction("Legacy Faction", "A faction of legendary wrestlers", true);
    factionService.addMemberToFaction(legacyFaction.getId(), johnCena.getId());
    factionService.addMemberToFaction(legacyFaction.getId(), theRock.getId());
    legacyFaction.setLeader(johnCena);
    factionRepository.save(legacyFaction);

    log.info(
        "✅ Created {} wrestlers and {} factions with members",
        wrestlerRepository.count(),
        factionRepository.count());
  }

  private Wrestler createWrestler(
      String name, int stamina, int lowStamina, int health, int lowHealth) {
    Wrestler wrestler = new Wrestler();
    wrestler.setName(name);
    wrestler.setStartingStamina(stamina);
    wrestler.setLowStamina(lowStamina);
    wrestler.setStartingHealth(health);
    wrestler.setLowHealth(lowHealth);
    wrestler.setDeckSize(15);
    wrestler.setCreationDate(Instant.now());
    return wrestlerRepository.save(wrestler);
  }

  private Faction createFaction(String name, String description, boolean isActive) {
    Faction faction = new Faction();
    faction.setName(name);
    faction.setDescription(description);
    faction.setIsActive(isActive);
    faction.setCreationDate(Instant.now());
    faction.setFormedDate(Instant.now());
    return factionRepository.save(faction);
  }
}
