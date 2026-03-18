package com.github.javydreamercsw.management.ui.view.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.AbstractE2ETest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

class ExpansionManagementE2EIT extends AbstractE2ETest {

  @Test
  void testToggleExpansionAndVerifyFiltering() {
    // 1. Navigate to Admin and select Expansion Management tab
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();

    WebElement tab =
        waitForVaadinElement(
            driver, By.xpath("//vaadin-tab[contains(text(), 'Expansion Management')]"));
    clickElement(tab);
    
    // Wait for grid to load expansions
    waitForVaadinElement(driver, By.xpath("//vaadin-grid-cell-content[contains(., 'Extreme Pack')]"));

    // 2. Find the checkbox for 'Extreme Pack' and disable it
    // Note: This assumes 'Extreme Pack' is in the grid.
    WebElement extremeCheckbox = 
        waitForVaadinElement(driver, By.xpath("//vaadin-grid-cell-content[contains(., 'Extreme Pack')]/..//vaadin-checkbox"));
    
    // Check if it's currently checked (enabled by default)
    if (extremeCheckbox.getAttribute("checked") != null) {
        clickElement(extremeCheckbox);
    }
    
    // Wait for notification
    waitForVaadinElement(driver, By.xpath("//vaadin-notification-card[contains(., 'disabled')]"));

    // 3. Navigate to Wrestler List and verify Rob Van Dam (EXTREME set) is hidden
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();
    
    // Verify 'Rob Van Dam' is NOT present
    assertThat(driver.findElements(By.xpath("//*[contains(text(), 'Rob Van Dam')]"))).isEmpty();

    // 4. Go back and re-enable it
    driver.get("http://localhost:" + serverPort + getContextPath() + "/admin");
    waitForVaadinClientToLoad();
    clickElement(waitForVaadinElement(driver, By.xpath("//vaadin-tab[contains(text(), 'Expansion Management')]")));
    
    extremeCheckbox = 
        waitForVaadinElement(driver, By.xpath("//vaadin-grid-cell-content[contains(., 'Extreme Pack')]/..//vaadin-checkbox"));
    
    if (extremeCheckbox.getAttribute("checked") == null) {
        clickElement(extremeCheckbox);
    }
    
    waitForVaadinElement(driver, By.xpath("//vaadin-notification-card[contains(., 'enabled')]"));

    // 5. Verify Rob Van Dam is back
    driver.get("http://localhost:" + serverPort + getContextPath() + "/wrestler-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Rob Van Dam')]"));
  }
}
