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
package com.github.javydreamercsw.management.ui.view;

import lombok.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Base class for video-documentation E2E tests. Selected by the {@code generate-videos} Maven
 * profile ({@code mvn verify -Pgenerate-videos}).
 *
 * <p>Video tests are intentionally separate from screenshot-only tests: they contain richer
 * workflows (scrolling, interactions, state transitions) that produce meaningful video content.
 *
 * <p><b>Usage pattern:</b>
 *
 * <pre>{@code
 * class MyFeatureVideoDocsE2ETest extends AbstractVideoDocsE2ETest {
 *
 *   @Test
 *   void testRecordMyFeature() {
 *     setVideoInfo("My Category", "My Feature", "my-feature-video");
 *
 *     navigateTo("my-route");
 *     waitForVaadinClientToLoad();
 *     captureCaption("Opening the My Feature view showing the full list.");
 *
 *     // interact — scroll, click, expand rows…
 *     clickElement(driver.findElement(By.id("some-item")));
 *     waitForVaadinClientToLoad();
 *     captureCaption("Detail panel open, showing item properties.");
 *   }
 * }
 * }</pre>
 */
public abstract class AbstractVideoDocsE2ETest extends AbstractDocsE2ETest {

  private String videoCategory;
  private String videoTitle;
  private String videoName;

  /**
   * Sets the video metadata for the current test. Must be called once per {@code @Test} method
   * before any interactions so the assembled MP4 gets the correct filename and manifest entry.
   */
  protected void setVideoInfo(
      @NonNull String category, @NonNull String title, @NonNull String videoName) {
    this.videoCategory = category;
    this.videoTitle = title;
    this.videoName = videoName;
  }

  @BeforeEach
  final void startVideoRecording(TestInfo testInfo) {
    videoCategory = null;
    videoTitle = null;
    videoName = null;
    String safeName =
        testInfo.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
    startVideoCapture(safeName);
  }

  @AfterEach
  final void stopVideoRecording() {
    if (videoName != null) {
      finishVideoCapture(videoCategory, videoTitle, videoName);
    } else {
      // setVideoInfo() was never called — abort silently without writing output
      finishVideoCapture("Uncategorized", "Unknown", "_discard_" + System.currentTimeMillis());
    }
  }
}
