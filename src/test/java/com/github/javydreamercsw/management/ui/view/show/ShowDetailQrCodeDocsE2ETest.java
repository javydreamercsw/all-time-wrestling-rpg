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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Docs screenshot test: verifies that clicking the QR share button on a match segment opens a
 * dialog displaying a QR code image with a LAN-accessible URL (not localhost).
 */
class ShowDetailQrCodeDocsE2ETest extends AbstractE2ETest {

  @Autowired private ShowRepository showRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;

  private Show testShow;
  private Segment testSegment;

  @BeforeEach
  void setupQrTestData() {
    ShowType showType =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setExpectedMatches(3);
                  st.setExpectedPromos(2);
                  return showTypeRepository.save(st);
                });

    testShow = new Show();
    testShow.setName("QR Code Test Show");
    testShow.setShowDate(LocalDate.now());
    testShow.setDescription("Show for QR code docs test");
    testShow.setType(showType);
    testShow.setUniverse(defaultUniverse);
    testShow = showRepository.save(testShow);

    Wrestler w1 = ensureWrestler("QR Test Alpha");
    Wrestler w2 = ensureWrestler("QR Test Beta");

    // Ensure a non-Promo segment type exists so the QR button is rendered.
    // Do not rely on DataInitializer — DatabaseCleaner truncates segment_type before each test
    // and DataInitializer's skipIfNotEmpty guard can prevent the reload. An invisible component
    // inside a Vaadin Grid ComponentRenderer is never sent to the browser DOM, so
    // waitForVaadinElement would time out if the type happened to be "Promo".
    SegmentType matchType =
        segmentTypeRepository
            .findByName("One on One")
            .orElseGet(
                () -> {
                  SegmentType st = new SegmentType();
                  st.setName("One on One");
                  return segmentTypeRepository.save(st);
                });

    testSegment = new Segment();
    testSegment.setShow(testShow);
    testSegment.setSegmentType(matchType);
    testSegment.addParticipant(w1);
    testSegment.addParticipant(w2);
    testSegment.setSegmentOrder(1);
    testSegment = segmentRepository.saveAndFlush(testSegment);
  }

  @Test
  void qrShareButtonOpensDialogWithQrCodeImage() {
    driver.get(
        "http://localhost:" + serverPort + getContextPath() + "/show-detail/" + testShow.getId());
    waitForVaadinClientToLoad();
    // Wait for the segments grid wrapper and data to render before looking for component-column
    // buttons. This is required in production mode where Grid component columns render
    // asynchronously after the Vaadin client reports idle. BookerJourneyE2ETest uses the same
    // pattern successfully.
    waitForVaadinElement(driver, By.id("segments-grid-wrapper"));
    waitForGridToPopulate("segments-grid");

    // Click the QR share button for the test segment
    WebElement qrButton =
        waitForVaadinElement(driver, By.id("share-qr-button-" + testSegment.getId()));
    clickElement(qrButton);
    sleep(1500);

    // Verify the dialog opened (Vaadin 25 uses vaadin-dialog[opened], not vaadin-dialog-overlay)
    WebElement dialog = waitForVaadinElement(driver, By.cssSelector("vaadin-dialog[opened]"));
    assertTrue(dialog.isDisplayed(), "QR dialog should be open");

    // Verify the QR image is rendered
    WebElement qrImage =
        waitForVaadinElement(driver, By.cssSelector("img[alt='QR code for match']"));
    assertTrue(qrImage.isDisplayed(), "QR code image should be visible");
    String src = qrImage.getAttribute("src");
    assertTrue(
        src != null && src.startsWith("data:image/png;base64,"), "QR should be a base64 PNG");

    // Verify the URL shown is not localhost (must be LAN-accessible)
    WebElement urlLabel = dialog.findElement(By.cssSelector("span"));
    String displayedUrl = urlLabel.getText();
    assertFalse(
        displayedUrl.contains("localhost") || displayedUrl.contains("127.0.0.1"),
        "QR URL must use LAN IP, not localhost — got: " + displayedUrl);

    documentFeature(
        "Booker",
        "QR Code Match Share",
        "The QR share button on a match segment opens a dialog with a scannable QR code."
            + " The URL uses the machine's LAN IP address so phones on the same network"
            + " can navigate directly to the match page.",
        "booker-show-detail-qr-code-share");
  }

  private Wrestler ensureWrestler(final String name) {
    return wrestlerRepository
        .findByName(name)
        .orElseGet(
            () -> {
              Account account = new Account();
              String uid = name.replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
              account.setUsername(uid);
              account.setEmail(uid + "@example.com");
              account.setPassword("password");
              account = accountRepository.save(account);
              return wrestlerRepository.save(
                  Wrestler.builder()
                      .name(name)
                      .startingHealth(100)
                      .startingStamina(100)
                      .account(account)
                      .active(true)
                      .build());
            });
  }
}
