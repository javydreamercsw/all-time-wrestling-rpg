/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GameMechanicsDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  @BeforeEach
  void setup() {
    if (showRepository.count() == 0) {
      ShowType weekly = showTypeRepository.findByName("Weekly").get();
      ShowType ple = showTypeRepository.findByName("Premium Live Event (PLE)").get();

      Show show1 = new Show();
      show1.setName("Monday Night Chaos");
      show1.setShowDate(LocalDate.now().plusDays(1));
      show1.setType(weekly);
      showRepository.save(show1);

      Show show2 = new Show();
      show2.setName("All Time Genesis");
      show2.setShowDate(LocalDate.now().plusDays(6));
      show2.setType(ple);
      showRepository.save(show2);
    }

    if (segmentRepository.count() == 0) {
      Show show = showRepository.findAll().get(0);
      SegmentType matchType = segmentTypeRepository.findByName("One on One").get();
      List<Wrestler> wrestlers = wrestlerRepository.findAll();

      if (wrestlers.size() >= 2) {
        Segment segment = new Segment();
        segment.setShow(show);
        segment.setSegmentType(matchType);
        segment.setSegmentDate(java.time.Instant.now());
        segment.addParticipant(wrestlers.get(0));
        segment.addParticipant(wrestlers.get(1));
        segmentRepository.save(segment);
      }
    }
  }

  @Test
  void testCaptureCardListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/card-list");
    waitForVaadinClientToLoad();
    waitForText("Card List");

    documentFeature(
        "Game Mechanics",
        "Cards",
        "The heart of the ATW RPG battle system. Each card represents a move (Strike, Grapple,"
            + " Aerial, Throw) with specific health and stamina costs and damage effects.",
        "mechanic-cards");
  }

  @Test
  void testCaptureDeckListView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/deck-list");
    waitForVaadinClientToLoad();
    waitForText("Deck List");

    documentFeature(
        "Game Mechanics",
        "Decks",
        "Manage wrestler-specific decks. Strategy involves balancing high-damage finishers with"
            + " efficient setup moves and stamina-recovering taunts.",
        "mechanic-decks");
  }

  @Test
  void testCaptureWrestlerRankingsView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-rankings");
    waitForVaadinClientToLoad();
    waitForText("Rankings");

    documentFeature(
        "Dashboards",
        "Wrestler Rankings",
        "Track the momentum of every wrestler in the promotion. Rankings determine title"
            + " eligibility and positioning on show cards.",
        "dashboard-rankings");
  }

  @Test
  void testCaptureShowCalendarView() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/show-calendar");
    waitForVaadinClientToLoad();
    waitForText("Show Calendar");

    documentFeature(
        "Dashboards",
        "Show Calendar",
        "Plan ahead with the integrated calendar. View upcoming weekly shows and major Premium"
            + " Live Events at a glance.",
        "dashboard-calendar");
  }

  @Test
  void testCaptureFactionSynergy() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/faction-list");
    waitForVaadinClientToLoad();
    waitForText("Synergy");

    documentFeature(
        "Game Mechanics",
        "Faction Synergy",
        "Factions grow stronger as members work together. Affinity builds through shared matches"
            + " and victories, unlocking powerful synergy buffs like stamina recovery, finisher"
            + " damage, and resilience bonuses. High-affinity factions have a mechanical advantage"
            + " in match outcomes.",
        "mechanic-faction-synergy");
  }

  @Test
  void testCaptureRingsideActions() {
    // Navigate to a match.
    Segment segment = segmentRepository.findAll().stream().findFirst().orElse(null);
    if (segment != null) {
      driver.get("http://localhost:" + serverPort + getContextPath() + "/match/" + segment.getId());
      waitForVaadinClientToLoad();
      waitForText("Ringside Actions");

      documentFeature(
          "Game Mechanics",
          "Ringside Actions",
          "Managers and faction members can swing the match in your favor. Perform ringside actions"
              + " like coaching, distracting the referee, or sliding in a weapon, but be careful -"
              + " too much illegal interference will lead to ejections or disqualification!",
          "mechanic-ringside-actions");
    }
  }

  @Test
  void testCaptureWearAndTear() {
    // Navigate to a wrestler profile to show the physical condition.
    Wrestler wrestler = wrestlerRepository.findAll().stream().findFirst().orElse(null);
    if (wrestler != null) {
      driver.get(
          "http://localhost:"
              + serverPort
              + getContextPath()
              + "/wrestler-profile/"
              + wrestler.getId());
      waitForVaadinClientToLoad();
      waitForText("Physical Condition");

      documentFeature(
          "Game Mechanics",
          "Persistent Wear & Tear",
          "Matches take a toll on a wrestler's body. Physical condition degrades over time based"
              + " on match intensity (Extreme matches double the wear) and positioning (Main Events"
              + " are more demanding). Low condition results in health penalties and potential"
              + " forced retirement.",
          "mechanic-wear-and-tear");
    }
  }

  private void waitForText(String text) {
    waitForVaadinElement(
        driver, org.openqa.selenium.By.xpath("//*[contains(text(), '" + text + "')]"));
  }
}
