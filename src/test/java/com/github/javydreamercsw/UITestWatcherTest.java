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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

class UITestWatcherTest {

  @TempDir Path tempDir;

  private UITestWatcher watcher;
  private ExtensionContext context;

  @BeforeEach
  void setUp() throws NoSuchMethodException {
    watcher = new UITestWatcher(tempDir);
    context = mock(ExtensionContext.class);
    when(context.getTestClass()).thenReturn(Optional.of(UITestWatcherTest.class));
    when(context.getRequiredTestMethod())
        .thenReturn(UITestWatcherTest.class.getDeclaredMethod("setUp"));
  }

  @AfterEach
  void restoreDriver() {
    AbstractE2ETest.driver = null;
  }

  // ---------------------------------------------------------------------------
  // beforeTestExecution
  // ---------------------------------------------------------------------------

  @Test
  void beforeTestExecution_deletesExistingOutputRootDir() throws Exception {
    Path nested = tempDir.resolve("SomeTest").resolve("someMethod");
    Files.createDirectories(nested);
    Files.writeString(nested.resolve("artifact.png"), "data");

    watcher.beforeTestExecution(context);

    assertFalse(Files.exists(tempDir), "outputRootDir should be fully deleted");
  }

  @Test
  void beforeTestExecution_doesNotThrowWhenDirAlreadyAbsent() throws Exception {
    watcher.beforeTestExecution(context); // deletes tempDir
    assertDoesNotThrow(
        () -> watcher.beforeTestExecution(context)); // already gone — should be no-op
  }

  // ---------------------------------------------------------------------------
  // afterTestExecution — success path
  // ---------------------------------------------------------------------------

  @Test
  void afterTestExecution_onSuccess_withExistingOutputDir_deletesIt() throws Exception {
    Path outputDir = tempDir.resolve(UITestWatcherTest.class.getSimpleName()).resolve("setUp");
    Files.createDirectories(outputDir);
    Files.writeString(outputDir.resolve("artifact.png"), "data");

    when(context.getExecutionException()).thenReturn(Optional.empty());

    watcher.afterTestExecution(context);

    assertFalse(Files.exists(outputDir), "per-test output dir should be cleaned up on success");
  }

  @Test
  void afterTestExecution_onSuccess_withNoOutputDir_doesNothing() {
    when(context.getExecutionException()).thenReturn(Optional.empty());
    assertDoesNotThrow(() -> watcher.afterTestExecution(context));
  }

  // ---------------------------------------------------------------------------
  // afterTestExecution — failure path
  // ---------------------------------------------------------------------------

  @Test
  void afterTestExecution_onFailure_nonE2ETestInstance_doesNotCreateOutput() throws Exception {
    when(context.getExecutionException()).thenReturn(Optional.of(new RuntimeException("failure")));
    when(context.getTestInstance()).thenReturn(Optional.of(new Object()));

    watcher.afterTestExecution(context);

    Path outputDir = tempDir.resolve(UITestWatcherTest.class.getSimpleName()).resolve("setUp");
    assertFalse(Files.exists(outputDir), "non-E2E instance should produce no output");
  }

  @Test
  void afterTestExecution_onFailure_nullDriver_doesNotCreateOutput() throws Exception {
    AbstractE2ETest.driver = null;
    AbstractE2ETest mockTest = mock(AbstractE2ETest.class);

    when(context.getExecutionException()).thenReturn(Optional.of(new RuntimeException("failure")));
    when(context.getTestInstance()).thenReturn(Optional.of(mockTest));

    watcher.afterTestExecution(context);

    Path outputDir = tempDir.resolve(UITestWatcherTest.class.getSimpleName()).resolve("setUp");
    assertFalse(Files.exists(outputDir), "null driver should produce no output");
  }

  @Test
  void afterTestExecution_onFailure_withDriver_savesScreenshotAndPageSource() throws Exception {
    // Mock a driver that also implements TakesScreenshot
    WebDriver mockDriver =
        mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
    Path screenshotFile = tempDir.resolve("fake-screenshot.png");
    Files.writeString(screenshotFile, "fake-png-data");
    when(mockDriver.getPageSource()).thenReturn("<html>failure page</html>");
    when(((TakesScreenshot) mockDriver).getScreenshotAs(OutputType.FILE))
        .thenReturn(screenshotFile.toFile());

    AbstractE2ETest mockTest = mock(AbstractE2ETest.class);
    AbstractE2ETest.driver = mockDriver;

    when(context.getExecutionException())
        .thenReturn(Optional.of(new RuntimeException("test failure")));
    when(context.getTestInstance()).thenReturn(Optional.of(mockTest));
    when(context.getDisplayName()).thenReturn("testNarrateAndSummarize()");

    watcher.afterTestExecution(context);

    Path outputDir = tempDir.resolve(UITestWatcherTest.class.getSimpleName()).resolve("setUp");
    assertTrue(Files.exists(outputDir), "output dir should be created on failure");

    List<String> fileNames;
    try (var stream = Files.list(outputDir)) {
      fileNames = stream.map(p -> p.getFileName().toString()).toList();
    }
    assertTrue(fileNames.stream().anyMatch(f -> f.endsWith(".png")), "screenshot should be saved");
    assertTrue(
        fileNames.stream().anyMatch(f -> f.endsWith(".html")), "page source should be saved");

    // Verify page source content
    try (var stream = Files.list(outputDir)) {
      Path htmlFile = stream.filter(p -> p.toString().endsWith(".html")).findFirst().orElseThrow();
      assertEquals("<html>failure page</html>", Files.readString(htmlFile));
    }
  }
}
