package com.github.javydreamercsw.management.ui.view.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InboxViewE2ETest extends AbstractE2ETest {

  @Autowired private InboxRepository inboxRepository;

  @BeforeEach
  public void setUp() throws IOException {
    super.setup();
    inboxRepository.deleteAll();
  }

  @Test
  void testOpenInboxView() {
    driver.get("http://localhost:" + serverPort + "/inbox");
    assertEquals("Inbox", driver.getTitle());
  }

  @Test
  void testReadColumnNotPresent() {
    driver.get("http://localhost:" + serverPort + "/inbox");
    WebElement grid = driver.findElement(By.tagName("vaadin-grid"));
    List<WebElement> headers = grid.findElements(By.tagName("vaadin-grid-column"));
    for (WebElement header : headers) {
      String headerText =
          (String)
              ((JavascriptExecutor) driver).executeScript("return arguments[0].header", header);
      if (headerText != null) {
        assertFalse(headerText.equalsIgnoreCase("read"));
      }
    }
  }

  @Test
  void testHideReadCheckbox() {
    // Given
    InboxItem readItem = new InboxItem();
    readItem.setRead(true);
    readItem.setDescription("read");
    inboxRepository.save(readItem);

    InboxItem unreadItem = new InboxItem();
    unreadItem.setRead(false);
    unreadItem.setDescription("unread");
    inboxRepository.save(unreadItem);

    driver.get("http://localhost:" + serverPort + "/inbox");

    // When
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement grid =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));
    List<WebElement> rows = grid.findElements(By.tagName("vaadin-grid-cell-content"));
    assertEquals(2, rows.size());

    WebElement hideReadCheckbox =
        driver.findElement(By.xpath("//vaadin-checkbox[@label='Hide Read']"));
    hideReadCheckbox.click();

    // Then
    rows = grid.findElements(By.tagName("vaadin-grid-cell-content"));
    assertEquals(1, rows.size());
  }

  @Test
  void testDeleteSelectedButton() {
    // Given
    InboxItem item1 = new InboxItem();
    item1.setDescription("item1");
    inboxRepository.save(item1);

    InboxItem item2 = new InboxItem();
    item2.setDescription("item2");
    inboxRepository.save(item2);

    driver.get("http://localhost:" + serverPort + "/inbox");

    // When
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement grid =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));
    WebElement firstRow = grid.findElement(By.tagName("vaadin-grid-cell-content"));
    firstRow.click();

    WebElement deleteButton =
        driver.findElement(By.xpath("//vaadin-button[text()='Delete Selected']"));
    deleteButton.click();

    // Then
    assertEquals(1, inboxRepository.count());
  }
}
