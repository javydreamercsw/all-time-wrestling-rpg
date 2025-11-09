package com.github.javydreamercsw;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

@Slf4j
public class UITestWatcher implements AfterTestExecutionCallback {

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    if (context.getExecutionException().isPresent()) {
      context
          .getTestInstance()
          .ifPresent(
              testInstance -> {
                if (testInstance instanceof AbstractE2ETest) {
                  WebDriver driver = ((AbstractE2ETest) testInstance).driver;
                  if (driver != null) {
                    // Print the page source to the console
                    log.debug("--- Page Source on Failure ---");
                    String pageSource = driver.getPageSource();
                    log.debug(pageSource);
                    log.debug("--- End Page Source ---");

                    try {
                      // Create a directory for the test output
                      Path outputDir =
                          Paths.get(
                              "target", "test-failures", context.getRequiredTestMethod().getName());
                      Files.createDirectories(outputDir);

                      // Capture screenshot
                      File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                      Path screenshotPath = outputDir.resolve("failure.png");
                      Files.copy(screenshot.toPath(), screenshotPath);
                      log.debug("Screenshot saved to: {}", screenshotPath);

                      // Save page source to file
                      Path pageSourcePath = outputDir.resolve("failure.html");
                      Files.writeString(pageSourcePath, pageSource);
                      log.debug("Page source saved to: {}", pageSourcePath);

                    } catch (IOException e) {
                      log.error("Failed to capture screenshot or page source: {}", e.getMessage());
                    }
                  }
                }
              });
    }
  }
}
