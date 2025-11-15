package com.github.javydreamercsw;

import com.github.javydreamercsw.base.test.AbstractIntegrationTest;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;

@ExtendWith(UITestWatcher.class)
@Slf4j
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

  protected WebDriver driver;

  @Value("${server.servlet.context-path}")
  @Getter
  private String contextPath;

  @BeforeEach
  public void setup() throws java.io.IOException {
    WebDriverManager.chromedriver().setup();
    log.info("Waiting for application to be ready on port {}", serverPort);
    waitForAppToBeReady();
    log.info("Application is ready on port {}", serverPort);
    ChromeOptions options = new ChromeOptions();
    if (isHeadless()) {
      options.addArguments("--headless=new");
      options.addArguments("--disable-gpu");
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
    }
    if (driver == null) {
      driver = new ChromeDriver(options);
    }
  }

  @AfterEach
  public void teardown() {
    if (driver != null) {
      driver.quit();
    }
  }

  private boolean isHeadless() {
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
  private void waitForAppToBeReady() {
    int maxAttempts = 60;
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
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("vaadin-grid")));
  }

  /** Waits for the Vaadin client-side application to fully load. */
  protected void waitForVaadinClientToLoad() {
    WebDriverWait wait =
        new WebDriverWait(driver, Duration.ofSeconds(30)); // Increased timeout for Vaadin client

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
   * @param element the WebElement to scroll into view and click
   */
  protected void clickAndScrollIntoView(@NonNull WebElement element) {
    scrollIntoView(element);
    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
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
   * Returns all the data from a specific column in a Vaadin grid.
   *
   * @param grid the Vaadin grid WebElement
   * @param columnIndex the index of the column (0-based)
   * @return List of Strings representing the data in the specified column
   */
  protected List<String> getColumnData(@NonNull WebElement grid, int columnIndex) {
    List<WebElement> cells =
        grid.findElements(
            By.xpath(
                "//vaadin-grid-cell-content[count(ancestor::vaadin-grid-column) = "
                    + (columnIndex + 1)
                    + "]"));
    return cells.stream().map(WebElement::getText).collect(Collectors.toList());
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
                "return arguments[0].shadowRoot.querySelectorAll('[part~=\"row\"]')", grid);
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
}
