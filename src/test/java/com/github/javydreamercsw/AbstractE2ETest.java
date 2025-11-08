package com.github.javydreamercsw;

import com.github.javydreamercsw.base.test.AbstractIntegrationTest;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.net.HttpURLConnection;
import java.net.URL;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(UITestWatcher.class)
@Slf4j
public abstract class AbstractE2ETest extends AbstractIntegrationTest {

  protected WebDriver driver;
  protected static int serverPort;
  private static final ConfigurableApplicationContext context;

  static {
    serverPort = Integer.parseInt(System.getProperty("server.port", "9090"));
    String[] args = {
      "--server.port=" + serverPort, "--spring.profiles.active=test",
    };
    context = SpringApplication.run(Application.class, args);
    Runtime.getRuntime().addShutdownHook(new Thread(context::close));
  }

  @BeforeEach
  public void setup() throws java.io.IOException {
    WebDriverManager.chromedriver().setup();
    waitForAppToBeReady();
    ChromeOptions options = new ChromeOptions();
    if (isHeadless()) {
      options.addArguments("--headless=new");
      options.addArguments("--disable-gpu");
      options.addArguments("--window-size=1920,1080");
      options.addArguments("--no-sandbox");
      options.addArguments("--disable-dev-shm-usage");
    }
    driver = new ChromeDriver(options);
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

  private void waitForAppToBeReady() {
    int maxAttempts = 60;
    int attempt = 0;
    while (attempt < maxAttempts) {
      try {
        URL url = new URL("http://localhost:" + serverPort);
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

  protected void clickVaadinButton(@NonNull String vaadinSelector) {
    WebElement button = getVaadinElementInShadowRoot(vaadinSelector, "button");
    if (button != null) {
      button.click();
    } else {
      throw new RuntimeException("Vaadin button not found: " + vaadinSelector);
    }
  }

  protected void scrollIntoView(@NonNull WebElement element) {
    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
  }

  protected WebElement getVaadinElementInShadowRoot(
      @NonNull String vaadinSelector, @NonNull String shadowRootSelector) {
    return (WebElement)
        ((JavascriptExecutor) driver)
            .executeScript(
                "return"
                    + " document.querySelector(arguments[0]).shadowRoot.querySelector(arguments[1]);",
                vaadinSelector,
                shadowRootSelector);
  }
}
