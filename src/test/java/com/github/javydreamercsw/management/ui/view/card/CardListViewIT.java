package com.github.javydreamercsw.management.ui.view.card;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@EnabledIf("isNotionTokenAvailable")
@TestPropertySource(properties = "data.initializer.enabled=false")
public class CardListViewIT extends AbstractE2ETest {

  @Autowired private CardService cardService;
  @Autowired private DeckRepository deckRepository;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private CardSetService cardSetService;

  @BeforeEach
  public void setup() {
    super.setup();
    deckRepository.deleteAll();
    cardRepository.deleteAll();
    cardSetRepository.deleteAll();
    CardSet testSet = new CardSet();
    testSet.setName("TST");
    cardSetService.save(testSet);
  }

  @Test
  public void testCreateCard() {
    driver.get("http://localhost:8080/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Wait for the grid to be present and populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));

    // Find the input field and create button
    WebElement nameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(
                    "vaadin-text-field[placeholder='What do you want the card name to be?']")));
    nameField.sendKeys("New E2E Card");

    WebElement createButton = driver.findElement(By.xpath("//vaadin-button[text()='Create']"));
    createButton.click();

    // Verify the new card is in the grid
    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(20))
            .handleResultIf(result -> !((WebElement) result).getText().contains("New E2E Card"))
            .build();
    Failsafe.with(retryPolicy)
        .get(
            () -> {
              WebElement refreshedGrid = driver.findElement(By.tagName("vaadin-grid"));
              assertTrue(refreshedGrid.getText().contains("New E2E Card"), refreshedGrid.getText());
              return refreshedGrid;
            });
  }

  @Test
  public void testUpdateCard() {
    String cardName = "Card to Update";
    // First, create a card to update
    CardSet testSet =
        cardSetRepository.findAll().stream()
            .filter(set -> set.getName().equals("TST"))
            .findFirst()
            .get();
    Card card = new Card();
    card.setName(cardName);
    card.setSet(testSet);
    card.setDamage(1);
    card.setTarget(1);
    card.setStamina(1);
    card.setMomentum(1);
    card.setType("Strike");
    card.setCreationDate(java.time.Instant.now());
    cardRepository.save(card);

    driver.get("http://localhost:8080/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Find the grid and wait for it to be populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    wait.until(d -> !grid.findElements(By.tagName("vaadin-grid-cell-content")).isEmpty());

    // Verify the new card is in the grid before trying to edit it
    RetryPolicy<Void> retryPolicyCreate =
        RetryPolicy.<Void>builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(20))
            .build();
    Failsafe.with(retryPolicyCreate)
        .run(
            () -> {
              driver.findElement(
                  By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]"));
            });

    // Find the row for our card and click the edit button
    WebElement cardRow =
        grid.findElement(By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]"));

    WebElement editButton =
        cardRow.findElement(
            By.xpath(
                "./following-sibling::vaadin-grid-cell-content//vaadin-button[text()='Edit']"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", editButton);
    wait.until(ExpectedConditions.elementToBeClickable(editButton));
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", editButton);

    // The editor is now open. Find the name field, update it, and save.
    WebElement editorNameField =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("vaadin-text-field[data-testid='name-editor']")));
    String updatedName = cardName + " - Updated";
    wait.until(ExpectedConditions.elementToBeClickable(editorNameField));
    ((JavascriptExecutor) driver).executeScript("arguments[0].value = ''", editorNameField);
    editorNameField.sendKeys(updatedName);

    WebElement saveButton = grid.findElement(By.xpath("//vaadin-button[text()='Save']"));
    wait.until(ExpectedConditions.elementToBeClickable(saveButton));
    saveButton.click();

    // Verify the update
    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(10))
            .handleResultIf(result -> !((WebElement) result).getText().contains(updatedName))
            .build();
    Failsafe.with(retryPolicy)
        .get(
            () -> {
              WebElement refreshedGrid = driver.findElement(By.tagName("vaadin-grid"));
              assertTrue(refreshedGrid.getText().contains(updatedName), refreshedGrid.getText());
              return refreshedGrid;
            });
  }

  @Test
  public void testDeleteCard() {
    String cardName = "Card to Delete";
    // First, create a card to delete
    cardService.createCard(cardName);

    driver.get("http://localhost:8080/card-list");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

    // Find the grid and wait for it to be populated
    WebElement grid =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-grid")));
    wait.until(d -> !grid.findElements(By.tagName("vaadin-grid-cell-content")).isEmpty());

    // Find the row for our card and click the delete button
    WebElement cardRow = null;
    for (int i = 0; i < 20; i++) { // Try 20 times
      try {
        cardRow =
            grid.findElement(
                By.xpath("//vaadin-grid-cell-content[contains(., '" + cardName + "')]"));
        break; // Element found, exit loop
      } catch (org.openqa.selenium.NoSuchElementException e) {
        ((JavascriptExecutor) driver)
            .executeScript(
                "arguments[0].scrollIntoView(true);",
                grid.findElement(By.xpath("//vaadin-grid-cell-content[last()]")));
        try {
          Thread.sleep(500); // Wait for content to load
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
    if (cardRow == null) {
      fail("Card with name '" + cardName + "' not found in the grid after scrolling.");
    }
    WebElement deleteButton =
        cardRow.findElement(
            By.xpath(
                "./following-sibling::vaadin-grid-cell-content//vaadin-button[@data-testid='delete-button']"));
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", deleteButton);

    // The confirmation dialog should appear. Click "Delete".
    WebElement confirmDeleteButton =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(
                    "vaadin-confirm-dialog-overlay vaadin-button[slot='confirm-button']")));
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmDeleteButton);

    // Verify the card is gone
    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .withDelay(Duration.ofMillis(500))
            .withMaxDuration(Duration.ofSeconds(10))
            .handleResultIf(result -> ((WebElement) result).getText().contains(cardName))
            .build();
    Failsafe.with(retryPolicy)
        .get(
            () -> {
              WebElement refreshedGrid = driver.findElement(By.tagName("vaadin-grid"));
              assertFalse(
                  refreshedGrid.getText().contains(cardName),
                  "Card should have been deleted but was found: " + refreshedGrid.getText());
              return refreshedGrid;
            });
  }
}
