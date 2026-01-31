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
package com.github.javydreamercsw;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.config.TestE2ESecurityConfig;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import com.github.javydreamercsw.management.util.docs.DocEntry;
import com.github.javydreamercsw.management.util.docs.DocumentationManifest;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(UITestWatcher.class)
@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = Application.class)
@ActiveProfiles(value = "e2e", inheritProfiles = false)
@Import(TestE2ESecurityConfig.class)
@WithMockUser(username = "admin", roles = "ADMIN")
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

  protected WebDriver driver;
  @LocalServerPort protected int serverPort;

  @Value("${server.servlet.context-path}")
  @Getter
  private String contextPath;

  private int screenshotCounter = 0;
  protected Path testArtifactsDir;

  private static final AtomicInteger docOrder = new AtomicInteger(0);

  static {
    // Shutdown hook to write documentation manifest
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (Boolean.getBoolean("generate.docs")) {
                    try {
                      Path manifestPath = Paths.get("docs", "manifest.json");
                      log.info(
                          "Writing documentation manifest to: {}", manifestPath.toAbsolutePath());
                      DocumentationManifest.write(manifestPath);
                    } catch (IOException e) {
                      log.error("Failed to write documentation manifest", e);
                    }
                  }
                }));
  }

  protected String getUsername() {
    return "admin";
  }

  protected String getPassword() {
    return "admin123";
  }

  @BeforeEach
  public void setup(TestInfo testInfo) {
    // Clear all caches
    if (cacheManager != null) {
      cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    WebDriverManager.chromedriver().setup();
    log.info("Waiting for application to be ready on port {}", serverPort);
    waitForAppToBeReady();
    log.info("Application is ready on port {}", serverPort);

    // Create artifact directory
    screenshotCounter = 0;
    try {
      String testClassName =
          testInfo.getTestClass().map(Class::getSimpleName).orElse("unknown-class");
      String testMethodName =
          testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown-method");
      testArtifactsDir = Paths.get("target", "test-failures", testClassName, testMethodName);
      // Clean up directory from previous runs
      if (Files.exists(testArtifactsDir)) {
        FileUtils.cleanDirectory(testArtifactsDir.toFile());
      }
      Files.createDirectories(testArtifactsDir);
    } catch (IOException e) {
      log.error("Unable to create test artifact directory", e);
      testArtifactsDir = null; // So we don't try to use it
    }

    ChromeOptions options = new ChromeOptions();
    if (isHeadless()) {
      options.addArguments("--headless=new");
    }
    options.addArguments("--disable-gpu");
    if (Boolean.getBoolean("generate.docs")) {
      // Consistent size for documentation screenshots
      options.addArguments("--window-size=1920,1080");
    } else {
      options.addArguments("--window-size=1920,1080");
    }
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--reduce-security-for-testing");
    options.addArguments("--disable-notifications");
    options.addArguments("--disable-save-password-bubble");
    options.addArguments("--disable-infobars");
    options.addArguments("--disable-extensions");

    Map<String, Object> prefs = new HashMap<>();
    prefs.put("credentials_enable_service", false);
    prefs.put("profile.password_manager_enabled", false);
    prefs.put("profile.password_manager_leak_detection", false);
    options.setExperimentalOption("prefs", prefs);
    options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});

    driver = new ChromeDriver(options);
    login();
  }

  protected void documentFeature(
      @NonNull String category,
      @NonNull String title,
      @NonNull String description,
      @NonNull String screenshotName) {
    if (Boolean.getBoolean("generate.docs")) {
      takeDocScreenshot(screenshotName);
      String id = category.toLowerCase().replace(" ", "-") + "-" + screenshotName;
      String relativeImagePath =
          "screenshots/"
              + (screenshotName.endsWith(".png") ? screenshotName : screenshotName + ".png");

      DocumentationManifest.addEntry(
          new DocEntry(
              id,
              category,
              title,
              description,
              relativeImagePath,
              docOrder.getAndIncrement() * 10));
    }
  }

  protected void takeDocScreenshot(@NonNull String fileName) {
    if (Boolean.getBoolean("generate.docs")) {
      try {
        Path docsDir = Paths.get("docs", "screenshots");
        if (!Files.exists(docsDir)) {
          Files.createDirectories(docsDir);
        }
        String screenshotName = fileName.endsWith(".png") ? fileName : fileName + ".png";
        Path destFile = docsDir.resolve(screenshotName);
        takeScreenshot(destFile.toString());
        log.info("Documentation screenshot saved: {}", destFile);
      } catch (IOException e) {
        log.error("Failed to save documentation screenshot", e);
      }
    }
  }

  protected void login() {
    login(getUsername(), getPassword());
  }

  protected void login(@NonNull String username, @NonNull String password) {
    driver.get("http://localhost:" + serverPort + getContextPath() + "/login");
    waitForAppToBeReady();
    takeSequencedScreenshot("on-login-page");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120));
    WebElement loginFormHost =
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("vaadinLoginFormWrapper")));

    String os = System.getProperty("os.name").toLowerCase();

    Keys modifier = os.contains("mac") ? Keys.COMMAND : Keys.CONTROL;

    WebElement usernameField = loginFormHost.findElement(By.id("vaadinLoginUsername"));

    WebElement usernameInput =
        (WebElement)
            ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].querySelector('input');", usernameField);

    if (usernameInput == null) usernameInput = usernameField;

    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new CustomEvent('input',"
                + " { bubbles: true })); arguments[0].dispatchEvent(new CustomEvent('change', {"
                + " bubbles: true }));",
            usernameInput,
            username);

    WebElement passwordField = loginFormHost.findElement(By.id("vaadinLoginPassword"));

    WebElement passwordInput =
        (WebElement)
            ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].querySelector('input');", passwordField);

    if (passwordInput == null) passwordInput = passwordField;

    ((JavascriptExecutor) driver)
        .executeScript(
            "arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new CustomEvent('input',"
                + " { bubbles: true })); arguments[0].dispatchEvent(new CustomEvent('change', {"
                + " bubbles: true }));",
            passwordInput,
            password);

    takeSequencedScreenshot("after-filling-credentials");
    WebElement signInButton =
        loginFormHost.findElement(By.cssSelector("vaadin-button[slot='submit']"));
    clickElement(signInButton);
    waitForAppToBeReady();
    try {
      // Use a more robust check for successful login - presence of logout button or main layout
      wait.until(ExpectedConditions.presenceOfElementLocated(By.id("logout-button")));
      takeSequencedScreenshot("after-successful-login");
    } catch (Exception e) {
      log.error("Login failed for user: {}", username);
      log.error("Current URL: {}", driver.getCurrentUrl());
      takeSequencedScreenshot("on-login-failure");
      try {
        WebElement error = loginFormHost.findElement(By.cssSelector("div[part='error-message']"));
        if (error.isDisplayed()) {
          log.error("Login error message: {}", error.getText());
        }
      } catch (Exception ignored) {
        log.error("Could not find error message element.");
      }
      throw e;
    }
  }

  @AfterEach
  public void teardown() {
    if (driver != null) {
      driver.quit();
    }
    // Clear the system property after each test to ensure isolation
    System.clearProperty("simulateFailure");
  }

  protected boolean isHeadless() {
    String headlessProp = System.getProperty("headless");
    String headlessEnv = System.getenv("HEADLESS");
    String githubActions = System.getenv("GITHUB_ACTIONS");
    if (headlessProp != null) {
      return headlessProp.equalsIgnoreCase("true");
    }
    if (headlessEnv != null) {
      return headlessEnv.equalsIgnoreCase("true");
    }
    // Default to headless in CI
    return "true".equalsIgnoreCase(githubActions);
  }

  /** Waits for the application to be ready by polling the root URL. */
  protected void waitForAppToBeReady() {
    int maxAttempts = 300; // Increased from 60 to handle slow production builds
    int attempt = 0;
    while (attempt < maxAttempts) {
      try {
        URL url = new URL("http://localhost:" + serverPort + getContextPath());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(1000);
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
          return;
        }
      } catch (Exception e) {
        // Ignore and retry
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ignored) {
      }
      attempt++;
    }
    throw new RuntimeException(
        "Application did not start within timeout. "
            + "Please check if the server is running on port "
            + serverPort);
  }

  /**
   * Waits for the Vaadin components to load by checking for the presence of a vaadin-grid element.
   *
   * @param driver the WebDriver instance
   */
  protected void waitForVaadinToLoad(@NonNull WebDriver driver) {
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
  }

  protected WebElement waitForVaadinElement(@NonNull WebDriver driver, @NonNull By selector) {
    takeSequencedScreenshot("before-wait-for-element");
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(120)); // Increased from 60
    return wait.until(ExpectedConditions.presenceOfElementLocated(selector));
  }

  protected void waitForGridToPopulate(@NonNull String gridId) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    wait.until(
        d -> {
          try {
            WebElement grid = d.findElement(By.id(gridId));
            return getGridSize(grid) > 0;
          } catch (Exception e) {
            return false;
          }
        });
  }

  protected void waitForNotification(@NonNull String text) {
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(
        ExpectedConditions.presenceOfElementLocated(
            By.xpath("//vaadin-notification-card[contains(., '" + text + "')]")));
  }

  /** Waits for the Vaadin client-side application to fully load. */
  protected void waitForVaadinClientToLoad() {
    WebDriverWait wait =
        new WebDriverWait(driver, Duration.ofSeconds(60)); // Increased timeout for Vaadin client

    // Wait for document.readyState to be 'complete'
    wait.until(
        webDriver ->
            Objects.equals(
                ((JavascriptExecutor) webDriver).executeScript("return document.readyState"),
                "complete"));

    // Wait for the main Vaadin app layout element to be present
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-app-layout")));
  }

  /**
   * Scrolls the given WebElement into view and clicks it using JavaScript.
   *
   * @param selector the WebElement to scroll into view and click
   */
  protected void clickElement(@NonNull By selector) {
    clickElement(driver.findElement(selector));
  }

  /**
   * Scrolls the given WebElement into view and clicks it using JavaScript.
   *
   * @param element the WebElement to scroll into view and click
   */
  protected void clickElement(@NonNull WebElement element) {
    scrollIntoView(element);
    takeSequencedScreenshot("before-click");
    // First, wait for the element to be clickable.
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.elementToBeClickable(element));
    // Then, use JavaScript to click to bypass potential interception by other elements.
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    takeSequencedScreenshot("after-click");
  }

  protected void click(@NonNull String tagName, @NonNull String text) {
    try {
      WebElement element =
          waitForVaadinElement(driver, By.xpath("//" + tagName + "[text()='" + text + "']"));
      clickElement(element);
    } catch (Exception e) {
      WebElement element =
          waitForVaadinElement(driver, By.xpath("//div[@role='tab'][text()='" + text + "']"));
      clickElement(element);
    }
  }

  protected void clickButtonByText(@NonNull String text) {
    WebElement element =
        waitForVaadinElement(driver, By.xpath("//vaadin-button[text()='" + text + "']"));
    clickElement(element);
  }

  /**
   * Scrolls the given WebElement into view using JavaScript.
   *
   * @param element the WebElement to scroll into view
   */
  protected void scrollIntoView(@NonNull WebElement element) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
  }

  /**
   * Selects an item from a Vaadin ComboBox by clicking it and selecting the item from the overlay.
   *
   * @param comboBox the Vaadin ComboBox WebElement
   * @param itemText the text of the item to select
   */
  protected void selectFromVaadinComboBox(@NonNull WebElement comboBox, @NonNull String itemText) {
    log.info("Selecting item '{}' from ComboBox", itemText);

    // Target the internal input element for typing
    WebElement input =
        (WebElement)
            ((JavascriptExecutor) driver)
                .executeScript(
                    "return arguments[0].querySelector('input') ||"
                        + " arguments[0].shadowRoot.querySelector('input');",
                    comboBox);

    if (input == null) {
      log.warn("Could not find internal input for ComboBox, falling back to direct sendKeys");
      input = comboBox;
    }

    // Clear and type
    input.click();
    input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
    input.sendKeys(itemText);

    // Wait for the overlay to appear and the item to be clickable
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    try {
      wait.until(
          d ->
              (Boolean)
                  ((JavascriptExecutor) d)
                      .executeScript(
                          "const text = arguments[0];const items ="
                              + " Array.from(document.querySelectorAll('vaadin-combo-box-item,"
                              + " vaadin-item'));const item = items.find(i => i.textContent.trim()"
                              + " === text || i.textContent.trim().includes(text));if (item) {"
                              + " item.click(); return true; }return false;",
                          itemText));
    } catch (Exception e) {
      log.error("Failed to select '{}' via overlay, attempting ENTER key", itemText);
      input.sendKeys(Keys.ENTER);
    }

    takeSequencedScreenshot("after-select");
  }

  protected void selectFromVaadinComboBox(@NonNull String comboBoxId, @NonNull String itemText) {
    WebElement comboBox = waitForVaadinElement(driver, By.id(comboBoxId));
    selectFromVaadinComboBox(comboBox, itemText);
  }

  /**
   * Returns all the data from a specific column in a Vaadin grid.
   *
   * @param grid the Vaadin grid WebElement
   * @param columnIndex the index of the column (0-based)
   * @return List of Strings representing the data in the specified column
   */
  protected List<String> getColumnData(@NonNull WebElement grid, int columnIndex) {
    return getGridRows(grid).stream()
        .map(
            row -> {
              List<WebElement> cells = row.findElements(By.cssSelector("[part~='cell']"));
              return cells.size() > columnIndex ? cells.get(columnIndex).getText() : "";
            })
        .collect(Collectors.toList());
  }

  /**
   * Returns the number of items in the grid.
   *
   * @param grid the Vaadin grid WebElement
   * @return the number of items
   */
  protected int getGridSize(@NonNull WebElement grid) {
    return getGridRows(grid).size();
  }

  /**
   * Returns all the row elements of a Vaadin grid.
   *
   * @param grid the Vaadin grid WebElement
   * @return List of WebElements, each representing a row
   */
  protected List<WebElement> getGridRows(@NonNull WebElement grid) {
    return (List<WebElement>)
        ((JavascriptExecutor) driver)
            .executeScript(
                "var items = arguments[0].shadowRoot.getElementById('items');"
                    + " return items ? items.children : [];",
                grid);
  }

  protected List<WebElement> getGridRows(@NonNull String gridId) {
    WebElement grid = waitForVaadinElement(driver, By.id(gridId));
    return getGridRows(grid);
  }

  protected void takeSequencedScreenshot(@NonNull String context) {
    if (driver != null && testArtifactsDir != null) {
      screenshotCounter++;
      String screenshotName = String.format("%03d-%s.png", screenshotCounter, context);
      Path destFile = testArtifactsDir.resolve(screenshotName);
      takeScreenshot(destFile.toString());
    }
  }

  protected void takeScreenshot(@NonNull String filePath) {
    File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
    try {
      FileUtils.copyFile(scrFile, new File(filePath));
      log.info("Screenshot saved to: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save screenshot to: {}", filePath, e);
    }
  }

  protected void takeElementScreenshot(@NonNull WebElement element, @NonNull String filePath) {
    File scrFile = element.getScreenshotAs(OutputType.FILE);
    try {
      FileUtils.copyFile(scrFile, new File(filePath));
      log.info("Screenshot saved to: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save screenshot to: {}", filePath, e);
    }
  }

  protected void savePageSource(@NonNull String filePath) {
    try {
      FileUtils.writeStringToFile(new File(filePath), driver.getPageSource(), "UTF-8");
      log.info("Page source saved to: {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save page source to: {}", filePath, e);
    }
  }

  protected void assertGridContains(@NonNull String gridId, @NonNull String expectedText) {
    WebElement grid = waitForVaadinElement(driver, By.id(gridId));
    boolean found = false;
    for (WebElement gridRow : getGridRows(grid)) {
      if (gridRow.getText().contains(expectedText)) {
        found = true;
        break;
      }
    }

    assertTrue(found, "Grid '" + gridId + "' does not contain '" + expectedText + "'.");
  }

  /**
   * Selects an item from a Vaadin MultiSelectComboBox by opening it and scrolling through the
   * items.
   *
   * @param comboBox the Vaadin MultiSelectComboBox WebElement
   * @param itemText the text of the item to select
   */
  protected void selectFromVaadinMultiSelectComboBox(
      @NonNull WebElement comboBox, @NonNull String itemText) {
    log.info("Selecting item '{}' from MultiSelectComboBox", itemText);

    // 1. Target the internal input element for typing to filter
    WebElement input =
        (WebElement)
            ((JavascriptExecutor) driver)
                .executeScript(
                    "return arguments[0].querySelector('input') ||"
                        + " arguments[0].shadowRoot.querySelector('input');",
                    comboBox);

    if (input != null) {
      input.click();
      input.sendKeys(itemText);
    } else {
      // Fallback: just open it
      ((JavascriptExecutor) driver).executeScript("arguments[0].opened = true;", comboBox);
    }

    // 2. Wait for the item to appear and click it via JS
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    try {
      wait.until(
          d -> {
            return (Boolean)
                ((JavascriptExecutor) d)
                    .executeScript(
                        "const text = arguments[0];const items ="
                            + " Array.from(document.querySelectorAll('vaadin-multi-select-combo-box-item,"
                            + " vaadin-combo-box-item, vaadin-item'));const item = items.find(i =>"
                            + " i.textContent.trim() === text ||"
                            + " i.textContent.trim().includes(text));if (item) { item.click();"
                            + " return true; }return false;",
                        itemText);
          });

      // Give it a moment to register the selection
      try {
        Thread.sleep(500);
      } catch (InterruptedException ignored) {
      }
    } catch (Exception e) {
      log.error("Failed to find or click item '{}' in MultiSelectComboBox", itemText, e);
      takeSequencedScreenshot("failed-to-select-multi-item");
      throw e;
    }
    takeSequencedScreenshot("after-multi-select");
  }

  /**
   * Selects an item from a Vaadin MenuBar by clicking the main button and selecting the item from
   * the overlay.
   *
   * @param menuBar the Vaadin MenuBar WebElement
   * @param itemText the text of the item to select
   */
  protected void selectFromVaadinMenuBar(@NonNull WebElement menuBar, @NonNull String itemText) {
    log.info("Selecting item '{}' from MenuBar", itemText);
    // 1. Find and click the main button of the MenuBar
    WebElement mainButton = menuBar.findElement(By.tagName("vaadin-menu-bar-button"));
    clickElement(mainButton);

    // 2. Wait and find the item by text in the DOM and click it via JS
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    try {
      wait.until(
          d -> {
            return (Boolean)
                ((JavascriptExecutor) d)
                    .executeScript(
                        "const text = arguments[0];const item ="
                            + " Array.from(document.querySelectorAll('vaadin-context-menu-item,"
                            + " vaadin-menu-bar-item, vaadin-item')).find(item =>"
                            + " item.textContent.trim() === text ||"
                            + " item.textContent.trim().includes(text));if (item) { item.click();"
                            + " return true; }return false;",
                        itemText);
          });
    } catch (Exception e) {
      log.error("Failed to find or click item '{}' in MenuBar", itemText, e);
      takeSequencedScreenshot("failed-to-select-menu-item");
      throw e;
    }
    takeSequencedScreenshot("after-menu-select");
  }

  protected void logout() {
    driver.get("http://localhost:" + serverPort + getContextPath());
    waitForVaadinElement(driver, By.id("logout-button"));
    WebElement logoutButton = driver.findElement(By.id("logout-button"));
    clickElement(logoutButton);
  }

  protected void clearField(@NonNull WebElement field) {
    takeSequencedScreenshot("before-clear");

    // Determine the correct modifier key for the current OS
    String os = System.getProperty("os.name").toLowerCase();
    Keys modifier = os.contains("mac") ? Keys.COMMAND : Keys.CONTROL;

    // Use keyboard shortcuts to clear the field, as .clear() is unreliable with Vaadin
    String selectAll = Keys.chord(modifier, "a");
    field.sendKeys(selectAll);
    field.sendKeys(Keys.BACK_SPACE);
    takeSequencedScreenshot("after-clear");
  }

  protected String getVaadinTextFieldErrorMessage(@NonNull String textFieldId) {
    WebElement textFieldElement = driver.findElement(By.id(textFieldId));

    // Check if the field is invalid
    Boolean isInvalid =
        (Boolean)
            ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].hasAttribute('invalid');", textFieldElement);

    if (isInvalid) {
      // Access the shadow root of the vaadin-text-field
      SearchContext shadowRoot = textFieldElement.getShadowRoot();

      // Find the error message element within the shadow root
      List<WebElement> errorMessageElements =
          shadowRoot.findElements(By.cssSelector("[part='error-message']"));

      if (!errorMessageElements.isEmpty()) {
        WebElement errorMessageElement = errorMessageElements.get(0);
        if (errorMessageElement.isDisplayed()) {
          return errorMessageElement.getText();
        }
      }
    }
    return null; // Or throw an exception if the field is not invalid
  }
}
