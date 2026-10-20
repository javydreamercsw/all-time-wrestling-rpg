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
package com.github.javydreamercsw.management.ui.view.tournament;

import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.team.TeamSize;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Docs screenshots for the team-tournament creation wizard (ATW-01g2). The TEAM championship is
 * custom content (the official tag title is not cataloged until ~April 2027), so setup creates it
 * through the title service; teams ride the seeded REGISTRY teams (teams.json).
 */
class TournamentTeamWizardDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TitleService titleService;
  @Autowired private TeamService teamService;
  @Autowired private UniverseService universeService;

  private static final String TAG_TITLE_NAME = "Docs World Tag Team Championship";

  @BeforeEach
  void seedTeamTitle() {
    // Custom content: created through the normal service (save() stamps expansionCode=CUSTOM),
    // never via the championship catalog.
    if (titleService.findByName(TAG_TITLE_NAME).isEmpty()) {
      Long universeId = universeService.findAll().iterator().next().getId();
      Title tagTitle =
          titleService.createTitle(
              TAG_TITLE_NAME,
              "Dusty-Cup-style tag team tournament prize (docs fixture).",
              WrestlerTier.ROOKIE,
              ChampionshipType.TEAM,
              null,
              universeId);
      tagTitle.setTeamSize(TeamSize.DUO);
      titleService.save(tagTitle);
    }
  }

  @Test
  void captureTeamTournamentWizard() {
    navigateToAndWaitForElement("tournament-list", By.tagName("vaadin-grid"));
    clickButtonByText("New Tournament");
    waitForVaadinElement(driver, By.id("wizard-tournament-name"));

    // Fill the details step: name, format, and the custom TEAM title.
    WebElement nameField = driver.findElement(By.id("wizard-tournament-name"));
    nameField.sendKeys("Dusty Classic");
    nameField.sendKeys(Keys.TAB);
    selectFromVaadinComboBox(driver.findElement(By.id("wizard-format")), "Single Elimination");
    selectFromVaadinComboBox(driver.findElement(By.id("wizard-linked-title")), "Docs World Tag");

    // Switch to Tag Team entrants, advance to the seeding step, and switch to Manual —
    // the "Select Teams" registry picker + one-off panel appear there (mirroring the
    // singles wrestler picker's placement and visibility).
    selectRadio("wizard-entrant-type", "Tag Team");
    waitForVaadinElement(driver, By.id("wizard-next")).click();
    WebElement seedingMode = waitForVaadinElement(driver, By.id("wizard-seeding-mode"));
    selectFromVaadinComboBox(seedingMode, "Manual (pick wrestlers)");
    waitForVaadinElement(driver, By.id("team-registry-picker"));
    selectTeamsInRegistryPicker(driver.findElement(By.id("team-registry-picker")), 1);

    documentFeature(
        "Game Mechanics",
        "Team Tournaments",
        "Tournaments can be crowned with team champions: pick Tag Team or Trios entrants and"
            + " the seeding step swaps the wrestler picker for a team registry — active, seeded"
            + " teams of the declared size (DUO/TRIO) filtered by the gender restriction,"
            + " sorted by team fans. The linked championship must be a TEAM title, and the"
            + " payoff always puts it on the line: the winning team becomes the inaugural"
            + " champions.",
        "team-tournament-wizard");

    // The one-off assembly panel: member multi-select + save-to-registry checkbox.
    WebElement memberPicker = driver.findElement(By.id("wizard-one-off-members"));
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView({block:'center'});", memberPicker);

    documentFeature(
        "Game Mechanics",
        "Team Tournaments — One-Off Assembly",
        "Prefer a team that is not in the registry? Assemble one right in the wizard: pick its"
            + " members (wrestlers already committed to a picked registry team are excluded),"
            + " and the team name is generated from the lineup and uniquified on create. The"
            + " assembled team persists as a one-off (AD_HOC) team unless 'Save this team to"
            + " the team registry' is checked (default off), which flips it into the registry"
            + " for future tournaments.",
        "team-tournament-one-off-assembly");
  }

  /**
   * Selects a vaadin-radio-button inside the group with the given id. Radio labels render in the
   * shadow DOM (no text node in the light DOM), so the option is matched via its slotted label and
   * clicked through the element's checked property.
   */
  private void selectRadio(String groupId, String optionLabel) {
    waitForVaadinClientToLoad();
    String script =
        """
        const group = document.getElementById(arguments[0]);
        if (!group) throw new Error('Radio group not found: ' + arguments[0]);
        const radios = Array.from(group.querySelectorAll('vaadin-radio-button'));
        const target = radios.find(r => {
          const slot = r.shadowRoot && r.shadowRoot.querySelector('slot');
          const nodes = slot ? slot.assignedNodes() : [];
          const text = nodes.map(n => n.textContent).join('').trim();
          if (text) return text === arguments[1];
          return (r.innerText || r.textContent || '').trim() === arguments[1];
        });
        if (!target) {
          throw new Error('Radio option not found: ' + arguments[1]
            + ' in ' + radios.length + ' radios');
        }
        target.click();
        """;
    ((JavascriptExecutor) driver).executeScript(script, groupId, optionLabel);
    waitForVaadinClientToLoad();
  }

  /**
   * Selects the first N teams from the registry multi-select combo via the base class's proven
   * Vaadin-25 multiselect interaction (overlay open → wait for items → scroller selection-changed
   * event).
   */
  private void selectTeamsInRegistryPicker(WebElement combo, int count) {
    List<String> options = registryPickerOptionNames(combo);
    if (options.size() < count) {
      throw new IllegalStateException(
          "Registry picker has "
              + options.size()
              + " eligible teams — expected at least "
              + count
              + " (no eligible teams seeded?)");
    }
    for (String option : options.subList(0, count)) {
      selectFromVaadinMultiSelectComboBox(combo, option);
    }
  }

  /**
   * Opens the registry picker's overlay and reads the rendered option labels, so the caller can
   * pick specific teams by name through the base helper.
   */
  private List<String> registryPickerOptionNames(WebElement combo) {
    waitForVaadinClientToLoad();
    JavascriptExecutor js = (JavascriptExecutor) driver;
    scrollIntoView(combo);
    js.executeScript("arguments[0].opened = true;", combo);
    WebDriverWait wait = new WebDriverWait(driver, getWaitTimeout());
    wait.until(
        d ->
            js.executeScript(
                """
                const items = Array.from(document.querySelectorAll(\
                'vaadin-multi-select-combo-box-item'));\
                return items.some(el => {\
                  const r = el.getBoundingClientRect();\
                  return r.width > 0 && r.height > 0;\
                });\
                """));
    @SuppressWarnings("unchecked")
    List<String> names =
        (List<String>)
            js.executeScript(
                """
                return Array.from(document.querySelectorAll(\
                'vaadin-multi-select-combo-box-item'))\
                  .filter(el => el.getBoundingClientRect().height > 0)\
                  .map(el => (el.innerText || el.textContent).trim());\
                """);
    js.executeScript("arguments[0].opened = false;", combo);
    return names == null ? List.of() : names;
  }
}
