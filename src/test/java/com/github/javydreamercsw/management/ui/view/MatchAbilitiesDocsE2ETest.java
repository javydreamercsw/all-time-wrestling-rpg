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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.campaign.AbilityTiming;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.AbilityCategory;
import com.github.javydreamercsw.management.domain.wrestler.AbilityType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbility;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbilityRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

/** Docs screenshots for the ability reminders feature (ATW-erya). */
class MatchAbilitiesDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private WrestlerAbilityRepository wrestlerAbilityRepository;
  @Autowired private AccountRepository accountRepository;

  private WrestlerAbility ability(
      final Wrestler wrestler,
      final String name,
      final String description,
      final AbilityType type,
      final AbilityTiming timing,
      final String unlockCondition,
      final Integer maxUses) {
    WrestlerAbility a = new WrestlerAbility();
    a.setWrestler(wrestler);
    a.setName(name);
    a.setDescription(description);
    a.setAbilityType(type);
    a.setCategory(AbilityCategory.SIGNATURE);
    a.setTiming(timing);
    a.setUnlockCondition(unlockCondition);
    a.setMaxUses(maxUses);
    a.setDefault(true);
    return a;
  }

  @Test
  void testCaptureAbilityReminders() {
    Account playerAccount = accountRepository.findByUsername("player").orElseThrow();

    Wrestler player =
        wrestlerRepository.save(
            Wrestler.builder()
                .name("Docs Ability Player")
                .isPlayer(true)
                .gender(Gender.MALE)
                .account(playerAccount)
                .build());
    wrestlerAbilityRepository.save(
        ability(
            player,
            "Five Star Frog Splash",
            "Deal +2 damage on aerial attacks.",
            AbilityType.USES_LIMITED,
            AbilityTiming.OFFENSE,
            "event == 'AERIAL_ATTACK_SUCCESS'",
            2));
    wrestlerAbilityRepository.save(
        ability(
            player,
            "Educated Feet",
            "Reduce incoming strike damage by 1.",
            AbilityType.CONDITIONAL,
            AbilityTiming.DEFENSE,
            "event == 'ATTACK_INCOMING' && match.attackCardType == 'STRIKE'",
            null));

    Wrestler opponent =
        wrestlerRepository.save(
            Wrestler.builder().name("Docs Ability Opponent").gender(Gender.MALE).build());
    wrestlerAbilityRepository.save(
        ability(
            opponent,
            "Power Kickout",
            "Reroll one kickout die.",
            AbilityType.USES_LIMITED,
            AbilityTiming.PINNED,
            "event == 'KICKOUT_ROLL'",
            1));

    ShowType weekly = showTypeRepository.findByName("Weekly").orElseThrow();
    Show show = new Show();
    show.setName("Ability Reminders Docs Show");
    show.setShowDate(LocalDate.now().plusDays(2));
    show.setType(weekly);
    show.setUniverse(defaultUniverse);
    show = showRepository.save(show);

    SegmentType oneOnOne = segmentTypeRepository.findByName("One on One").orElseThrow();
    Segment segment = new Segment();
    segment.setShow(show);
    segment.setSegmentType(oneOnOne);
    segment.setSegmentDate(Instant.now());
    segment.addParticipant(player);
    segment.addParticipant(opponent);
    segment = segmentRepository.save(segment);

    login("player", "player123");
    navigateToAndWaitForElement("match/" + segment.getId(), By.id("match-view-" + segment.getId()));

    // Participant cards show compact ability chips for every wrestler.
    waitForVaadinElement(driver, By.cssSelector("[id^='ability-chip-']"));

    // Open the player's interactive abilities panel for the screenshot.
    clickElement(By.xpath("//vaadin-details-summary[contains(.,'Your Abilities')]"));
    waitForVaadinElement(driver, By.cssSelector("[id^='ability-mark-used-']"));

    documentFeature(
        "Game Mechanics",
        "Ability Reminders",
        """
        The match view reminds everyone at the table what each wrestler's abilities do.\
         Participant cards carry compact ability chips with trigger tooltips, and the player's\
         own panel shows when each ability fires (color-coded by offense/defense/pinned), an\
         advisory uses-left counter, and a Mark Used button that logs the usage into the match\
         notes — feeding the AI narration and match summary. Nothing is enforced: the table\
         remains the authority.\
        """,
        "match-ability-reminders");
  }
}
