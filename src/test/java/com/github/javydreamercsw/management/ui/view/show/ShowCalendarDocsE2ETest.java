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
package com.github.javydreamercsw.management.ui.view.show;

import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.springframework.beans.factory.annotation.Autowired;

class ShowCalendarDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  void setup() {
    dataInitializer.init();
  }

  @Test
  void testCaptureShowCalendarScreenshot() {
    navigateTo("show-calendar");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-full-calendar"));

    documentFeature(
        "Booker",
        "Show Calendar",
        "The Show Calendar provides a monthly view of every scheduled show in the promotion."
            + " Each show appears as a colour-coded event tile on its booked date. Click any"
            + " tile to jump directly to that show's detail page. Navigate between months"
            + " using the prev/next arrows, or click Today to snap back to the current date.",
        "show-calendar-overview");
  }

  @Tag("video")
  @Test
  void testRecordShowCalendarWalkthrough() {
    setVideoInfo("Booker", "Show Calendar Walkthrough", "show-calendar-walkthrough");

    navigateTo("show-calendar");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-full-calendar"));

    captureCaption(
        "Show Calendar — a monthly grid view of every booked show in the promotion."
            + " Each event tile shows the show name on its scheduled date, colour-coded"
            + " by show type. Today's date is highlighted so you always know where you are"
            + " in the booking calendar.",
        5000);

    // Navigate to next month to demonstrate navigation
    try {
      // The next-month button is the second icon-tertiary vaadin-button in the toolbar
      org.openqa.selenium.WebElement nextBtn =
          waitForVaadinElement(driver, By.xpath("(//vaadin-button[@theme='icon tertiary'])[2]"));
      captureCaption(
          "Use the Previous and Next arrows to page through months and plan ahead."
              + " The Today button snaps the view back to the current month — useful"
              + " when you've scrolled far into the future during long-range booking.",
          4500);
      clickElement(nextBtn);
      waitForVaadinClientToLoad();
      sleep(800);
    } catch (Exception e) {
      sleep(800);
    }

    captureCaption(
        "Click any show tile to navigate directly to that show's detail page where you"
            + " can add segments, reorder matches, and adjudicate results."
            + " The calendar is the fastest way to find and open a specific date's card.",
        4500);

    ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 150)");
    sleep(700);
    captureCaption(
        "Shows scheduled in future months appear here as soon as they're created in the"
            + " Show List. Plan your entire PPV season at a glance — weekly shows, special"
            + " events, and pay-per-view dates all land on the same calendar.",
        4000);

    sleep(1500);
  }
}
