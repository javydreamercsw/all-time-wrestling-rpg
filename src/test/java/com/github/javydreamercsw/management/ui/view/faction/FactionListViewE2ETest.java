package com.github.javydreamercsw.management.ui.view.faction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.AbstractE2ETest;
import com.github.javydreamercsw.management.domain.faction.Faction;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class FactionListViewE2ETest extends AbstractE2ETest {

  @Test
  void testCreateFaction() {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/faction-list");
    waitForVaadinClientToLoad();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the vaadin-grid to be visible
    waitForVaadinToLoad(driver);

    // Get the initial size of the grid
    long initialSize = factionService.count();

    // Click the "Create Faction" button
    WebElement createButton =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("create-faction-button")));
    wait.until(ExpectedConditions.elementToBeClickable(createButton));
    clickAndScrollIntoView(createButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Find the name field and enter a new faction name
    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-name")));
    nameField.sendKeys("Test Faction", Keys.TAB);

    // Find the save button and click it
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("save-button")));
    clickAndScrollIntoView(saveButton);

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Verify that the new faction appears in the grid
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(By.id("faction-grid"), "Test Faction"));

    assertEquals(initialSize + 1, factionService.count());
  }

  @Test
  void testEditFaction() {
    // Create a faction to edit
    Faction faction =
        Faction.builder().name("Faction to Edit").description("Original Description").build();
    factionService.save(faction);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/faction-list");
    waitForVaadinClientToLoad();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    // Wait for the vaadin-grid to be visible
    waitForVaadinToLoad(driver);

    // Find the "Edit" button for the faction and click it
    WebElement editButton = driver.findElement(By.id("edit-" + faction.getId()));
    scrollIntoView(editButton);
    wait.until(ExpectedConditions.elementToBeClickable(editButton));

    clickAndScrollIntoView(editButton);

    // Wait for the dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Find the name field and change the value
    WebElement nameField =
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("edit-name")));
    nameField.sendKeys(" Edited", Keys.TAB);

    // Find the save button and click it
    WebElement saveButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("save-button")));
    clickAndScrollIntoView(saveButton);

    // Verify that the faction is updated in the grid
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.id("faction-grid"), "Faction to Edit Edited"));

    Optional<Faction> updatedFaction = factionService.getFactionById(faction.getId());
    assertTrue(updatedFaction.isPresent());
    assertEquals("Faction to Edit Edited", updatedFaction.get().getName());
  }

  @Test
  void testDeleteFaction() {
    // Create a faction to delete
    Faction faction =
        Faction.builder().name("Faction to Delete").description("Description").build();
    factionService.save(faction);

    driver.get("http://localhost:" + serverPort + getContextPath() + "/faction-list");
    waitForVaadinClientToLoad();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

    long initialSize = factionService.count();

    // Wait for the vaadin-grid to be visible
    waitForVaadinToLoad(driver);

    // Find the "Delete" button for the faction and click it
    WebElement deleteButton = driver.findElement(By.id("delete-" + faction.getId()));
    scrollIntoView(deleteButton);
    wait.until(ExpectedConditions.elementToBeClickable(deleteButton));

    clickAndScrollIntoView(deleteButton);

    // Confirm the deletion in the dialog

    WebElement confirmDialogOverlay =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.tagName("vaadin-confirm-dialog-overlay")));

    WebElement dialogDeleteButton =
        confirmDialogOverlay.findElement(By.xpath(".//vaadin-button[text()='Delete']"));
    clickAndScrollIntoView(dialogDeleteButton);

    wait.until(
        ExpectedConditions.invisibilityOfElementLocated(
            By.tagName("vaadin-confirm-dialog-overlay")));

    // Verify that the faction is removed from the grid
    wait.until(
        ExpectedConditions.invisibilityOfElementWithText(
            By.id("faction-grid"), "Faction to Delete"));

    assertEquals(initialSize - 1, factionService.count());
  }

  @Test
  void testAddWrestlerToFaction() {
    // Create a faction and a wrestler
    Faction faction = factionService.save(Faction.builder().name("Faction with Wrestler").build());
    Wrestler wrestler =
        wrestlerService.save(
            Wrestler.builder()
                .name("Faction Wrestler")
                .deckSize(30)
                .startingHealth(100)
                .lowHealth(10)
                .startingStamina(100)
                .lowStamina(10)
                .build());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/faction-list");
    waitForVaadinClientToLoad();
    waitForVaadinToLoad(driver);
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    WebElement membersButton = driver.findElement(By.id("members-" + faction.getId()));
    scrollIntoView(membersButton);
    wait.until(ExpectedConditions.elementToBeClickable(membersButton));

    clickAndScrollIntoView(membersButton);

    // Wait for the members dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Select the wrestler from the ComboBox
    WebElement wrestlerComboBox =
        wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("add-member-wrestler-combo")));
    wrestlerComboBox.sendKeys(wrestler.getName());
    wrestlerComboBox.sendKeys(Keys.ENTER);

    // Click the "Add Member" button
    WebElement addMemberButton =
        wait.until(ExpectedConditions.elementToBeClickable(By.id("add-member-button")));
    clickAndScrollIntoView(addMemberButton);

    // Verify the wrestler is added to the members grid
    wait.until(
        ExpectedConditions.textToBePresentInElementLocated(
            By.id("members-grid"), wrestler.getName()));

    Optional<Faction> updatedFaction = factionService.getFactionByIdWithMembers(faction.getId());
    assertTrue(updatedFaction.isPresent());
    assertTrue(
        updatedFaction.get().getMembers().stream()
            .anyMatch(
                m -> {
                  Assertions.assertNotNull(m.getId());
                  return m.getId().equals(wrestler.getId());
                }));
  }

  @Test
  void testRemoveWrestlerFromFaction() {
    // Create a faction and a wrestler, and add the wrestler to the faction
    Faction faction = factionService.save(Faction.builder().name("Faction to Remove From").build());
    Wrestler wrestler =
        wrestlerService.save(
            Wrestler.builder()
                .name("Wrestler to Remove")
                .deckSize(30)
                .startingHealth(100)
                .lowHealth(10)
                .startingStamina(100)
                .lowStamina(10)
                .build());
    Assertions.assertNotNull(faction.getId());
    Assertions.assertNotNull(wrestler.getId());
    factionService.addMemberToFaction(faction.getId(), wrestler.getId());

    driver.get("http://localhost:" + serverPort + getContextPath() + "/faction-list");
    waitForVaadinClientToLoad();
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

    // Wait for the vaadin-grid to be visible
    waitForVaadinToLoad(driver);

    // Open the members dialog
    WebElement membersButton = driver.findElement(By.id("members-" + faction.getId()));
    scrollIntoView(membersButton);
    wait.until(ExpectedConditions.elementToBeClickable(membersButton));

    clickAndScrollIntoView(membersButton);

    // Wait for the members dialog to appear
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("vaadin-dialog-overlay")));

    // Find the "Remove" button for the wrestler and click it
    WebElement removeButton =
        wait.until(
            ExpectedConditions.elementToBeClickable(By.id("remove-member-" + wrestler.getId())));
    clickAndScrollIntoView(removeButton);

    // Verify the wrestler is removed from the members grid
    wait.until(
        ExpectedConditions.invisibilityOfElementWithText(
            By.id("members-grid"), wrestler.getName()));

    Optional<Faction> updatedFaction = factionService.getFactionByIdWithMembers(faction.getId());
    assertTrue(updatedFaction.isPresent());
    assertTrue(
        updatedFaction.get().getMembers().stream()
            .noneMatch(
                m -> {
                  Assertions.assertNotNull(m.getId());
                  return m.getId().equals(wrestler.getId());
                }));
  }
}
