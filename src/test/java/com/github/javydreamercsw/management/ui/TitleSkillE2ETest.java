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
package com.github.javydreamercsw.management.ui;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.TestUtils;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRuleRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReignRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

/** E2E tests for the ATW Extreme title skill button in the Match Detail (MatchView) screen. */
@Slf4j
public class TitleSkillE2ETest extends AbstractE2ETest {

  private static final String SHOW_TYPE_NAME = "Weekly";
  private static final String SEASON_NAME = "Title Skill E2E Season";
  private static final String TEMPLATE_NAME = "Continuum";
  private static final String NO_DQ_RULE = "No DQ";
  private static final String EFFECT_SCRIPT = "addMatchRule('No DQ'); setupWeaponCard()";

  @Autowired private AccountRepository accountRepository;
  @Autowired private TitleRepository titleRepository;
  @Autowired private TitleReignRepository titleReignRepository;
  @Autowired private SegmentRuleRepository segmentRuleRepository;

  private Title extremeTitle;
  private Wrestler championWrestler;
  private Wrestler challengerWrestler;
  private Segment titleSegment;
  private Segment nonTitleSegment;

  @BeforeEach
  public void setUpTitleSkillData() {
    // Full state reset — mirrors BookerJourneyE2ETest pattern to avoid cross-test contamination
    titleReignRepository
        .findAll()
        .forEach(
            reign -> {
              reign.setWonAtSegment(null);
              titleReignRepository.save(reign);
            });
    titleReignRepository.deleteAll();
    titleRepository
        .findAll()
        .forEach(
            title -> {
              title.setChampion(null);
              titleRepository.save(title);
            });
    segmentRepository.deleteAll();
    showRepository.deleteAll();
    wrestlerRepository.deleteAll();

    // Ensure minimal infrastructure (ShowType, Season, Template, SegmentType, No DQ rule)
    if (showTypeRepository.findByName(SHOW_TYPE_NAME).isEmpty()) {
      ShowType st = new ShowType();
      st.setName(SHOW_TYPE_NAME);
      st.setDescription("Weekly show");
      st.setExpectedMatches(3);
      st.setExpectedPromos(1);
      showTypeRepository.save(st);
    }
    if (seasonRepository.findByName(SEASON_NAME).isEmpty()) {
      Season s = new Season();
      s.setName(SEASON_NAME);
      seasonRepository.save(s);
    }
    if (showTemplateRepository.findByName(TEMPLATE_NAME).isEmpty()) {
      ShowTemplate t = new ShowTemplate();
      t.setName(TEMPLATE_NAME);
      t.setShowType(showTypeRepository.findByName(SHOW_TYPE_NAME).get());
      showTemplateRepository.save(t);
    }
    if (segmentTypeRepository.findByName("One on One").isEmpty()) {
      SegmentType st = new SegmentType();
      st.setName("One on One");
      st.setDescription("Singles match");
      segmentTypeRepository.save(st);
    }
    if (segmentRuleRepository.findByName(NO_DQ_RULE).isEmpty()) {
      SegmentRule rule = new SegmentRule();
      rule.setName(NO_DQ_RULE);
      rule.setDescription("No disqualification — anything goes.");
      segmentRuleRepository.save(rule);
    }

    // Player account is seeded by H2 migration V21 (password: player123)
    Account playerAccount = accountRepository.findByUsername("player").orElseThrow();

    // Wrestler owned by the player account — this is the triggering condition for the button
    championWrestler =
        TestUtils.createWrestler(
            "Title Skill Champion", "Test Champion", WrestlerTier.ROOKIE, playerAccount);
    championWrestler = wrestlerRepository.saveAndFlush(championWrestler);

    challengerWrestler =
        TestUtils.createWrestler(
            "Title Skill Challenger", "Test Challenger", WrestlerTier.ROOKIE, null);
    challengerWrestler = wrestlerRepository.saveAndFlush(challengerWrestler);

    // Title whose script triggers No DQ + weapon card
    extremeTitle = new Title();
    extremeTitle.setName("E2E Extreme Title");
    extremeTitle.setEffectScript(EFFECT_SCRIPT);
    extremeTitle.getChampion().add(championWrestler);
    extremeTitle = titleRepository.saveAndFlush(extremeTitle);

    // Create a show
    Show show =
        showService.createShow(
            "Title Skill E2E Show",
            "",
            showTypeRepository.findByName(SHOW_TYPE_NAME).get().getId(),
            LocalDate.now(),
            seasonRepository.findByName(SEASON_NAME).get().getId(),
            showTemplateRepository.findByName(TEMPLATE_NAME).get().getId(),
            null,
            null,
            null,
            null);

    SegmentType matchType = segmentTypeRepository.findByName("One on One").get();

    // Segment where the player's wrestler IS the champion — button should appear
    titleSegment = segmentService.createSegment(show, matchType, Instant.now(), new HashSet<>());
    titleSegment.setIsTitleSegment(true);
    titleSegment.setTitles(Set.of(extremeTitle));
    titleSegment.syncParticipants(List.of(championWrestler, challengerWrestler));
    segmentService.updateSegment(titleSegment);

    // Segment without the title flag — button must NOT appear even for the champion
    nonTitleSegment = segmentService.createSegment(show, matchType, Instant.now(), new HashSet<>());
    nonTitleSegment.setIsTitleSegment(false);
    nonTitleSegment.syncParticipants(List.of(championWrestler, challengerWrestler));
    segmentService.updateSegment(nonTitleSegment);
  }

  /**
   * The player who holds the contested title sees an enabled "Activate Title Skill" button in the
   * Match Info card.
   */
  @Test
  void testTitleSkillButtonVisibleForChampion() {
    login("player", "player123");
    navigateTo("match/" + titleSegment.getId());
    waitForVaadinClientToLoad();

    WebElement skillBtn =
        new WebDriverWait(driver, getWaitTimeout())
            .until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.id("activate-title-skill-button")));

    Assertions.assertNotNull(skillBtn, "Title skill button must be present for the champion");
    Assertions.assertTrue(skillBtn.isEnabled(), "Title skill button must be enabled");
  }

  /**
   * Clicking the button triggers the Groovy script (No DQ + weapon card), then disables the button
   * in-place.
   */
  @Test
  void testTitleSkillActivatesAndShowsWeaponCardNotification() {
    login("player", "player123");
    navigateTo("match/" + titleSegment.getId());
    waitForVaadinClientToLoad();

    WebElement skillBtn =
        new WebDriverWait(driver, getWaitTimeout())
            .until(ExpectedConditions.elementToBeClickable(By.id("activate-title-skill-button")));
    clickElement(skillBtn);

    // Weapon card notification must appear (persistent — duration 0)
    waitForNotification("weapons deck");

    // Button must be disabled immediately after activation
    WebElement btnAfter =
        new WebDriverWait(driver, getWaitTimeout())
            .until(
                ExpectedConditions.presenceOfElementLocated(By.id("activate-title-skill-button")));
    Assertions.assertFalse(
        btnAfter.isEnabled(), "Title skill button must be disabled after activation");
  }

  /**
   * After activation the {@code titleSkillUsed} flag is persisted. Reloading the match page must
   * not show the button again.
   */
  @Test
  void testTitleSkillButtonAbsentAfterActivationOnReload() {
    login("player", "player123");
    navigateTo("match/" + titleSegment.getId());
    waitForVaadinClientToLoad();

    clickElement(
        new WebDriverWait(driver, getWaitTimeout())
            .until(ExpectedConditions.elementToBeClickable(By.id("activate-title-skill-button"))));
    waitForNotification("weapons deck");

    // Reload — flag is now persisted in the database
    navigateTo("match/" + titleSegment.getId());
    waitForVaadinClientToLoad();

    List<WebElement> buttons = driver.findElements(By.id("activate-title-skill-button"));
    Assertions.assertTrue(
        buttons.isEmpty(),
        "Title skill button must not be rendered after the skill has already been used");
  }

  /** A player whose wrestler is in the match but is NOT the current champion sees no button. */
  @Test
  void testTitleSkillButtonNotShownForNonChampionParticipant() {
    // Reassign the title to the challenger so the player-owned wrestler is no longer champion
    extremeTitle.getChampion().clear();
    extremeTitle.getChampion().add(challengerWrestler);
    titleRepository.saveAndFlush(extremeTitle);

    login("player", "player123");
    navigateTo("match/" + titleSegment.getId());
    waitForVaadinClientToLoad();

    List<WebElement> buttons = driver.findElements(By.id("activate-title-skill-button"));
    Assertions.assertTrue(
        buttons.isEmpty(),
        "Title skill button must not appear when the player's wrestler is not the champion");
  }

  /** Even when the player is the champion, no button appears in a non-title match segment. */
  @Test
  void testTitleSkillButtonNotShownInNonTitleMatch() {
    login("player", "player123");
    navigateTo("match/" + nonTitleSegment.getId());
    waitForVaadinClientToLoad();

    List<WebElement> buttons = driver.findElements(By.id("activate-title-skill-button"));
    Assertions.assertTrue(
        buttons.isEmpty(),
        "Title skill button must not appear in a segment where no title is contested");
  }
}
