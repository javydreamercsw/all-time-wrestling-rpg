/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.ui.view.show;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

public class ShowPlanningViewE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;

  private Show testShow;

  @BeforeEach
  public void setupTestData() {
    ShowType showType =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setDescription("Weekly Show");
                  st.setExpectedMatches(3);
                  st.setExpectedPromos(2);
                  return showTypeRepository.saveAndFlush(st);
                });

    testShow = new Show();
    testShow.setName("Test Show for Planning View");
    testShow.setType(showType);
    testShow.setShowDate(LocalDate.now().plusDays(7));
    testShow.setDescription("Test Description");
    // ShowPlanningService rejects shows without a universe (ATW-e6z8).
    testShow.setUniverse(defaultUniverse);
    showRepository.save(testShow);
  }

  @Test
  public void testNavigateToShowPlanningView() {
    // Retry-enabled navigation — a login redirect race can swallow a plain driver.get()
    // and leave the browser on the Home view instead of the planning view.
    navigateToAndWaitForElement(
        "show-planning/" + testShow.getId(), By.id("select-show-combo-box"));

    WebElement comboBox = waitForVaadinElementVisible(By.id("select-show-combo-box"));
    assertNotNull(comboBox);
  }
}
