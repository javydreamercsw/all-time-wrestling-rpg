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
package com.github.javydreamercsw.base.config;

import jakarta.servlet.ServletContext;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Taskbar;
import java.awt.TrayIcon;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Integration with desktop features like System Tray and automatic browser launching. Only active
 * when 'atw.desktop.enabled' is set to true.
 */
@Component
@ConditionalOnProperty(name = "atw.desktop.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DesktopIntegration implements ApplicationListener<ApplicationReadyEvent> {

  private final ResourceLoader resourceLoader;

  @Autowired(required = false)
  private ServletContext servletContext;

  @Value("${server.port:8080}")
  private int defaultPort;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    if (java.awt.GraphicsEnvironment.isHeadless()) {
      log.warn("Desktop integration enabled but environment is headless. Skipping.");
      return;
    }

    int port = defaultPort;
    if (event.getApplicationContext() instanceof ServletWebServerApplicationContext serverContext) {
      port = serverContext.getWebServer().getPort();
    }

    String contextPath =
        event
            .getApplicationContext()
            .getEnvironment()
            .getProperty("server.servlet.context-path", "");

    if (contextPath.isEmpty() && servletContext != null) {
      contextPath = servletContext.getContextPath();
    }

    // Ensure context path starts with / if not empty, and doesn't end with /
    if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
      contextPath = "/" + contextPath;
    }
    if (contextPath.equals("/")) {
      contextPath = "";
    }

    String url = String.format("http://localhost:%d%s/", port, contextPath);
    log.info("Constructed application URL: {}", url);

    // Try to load icon for both Tray and Dock/Taskbar
    Image iconImage = loadIcon();

    if (iconImage != null) {
      setupDockIcon(iconImage);
      setupSystemTray(url, iconImage);
    }

    launchBrowser(url);
  }

  private Image loadIcon() {
    try {
      // Use the high-res source icon if available, otherwise fallback to the PNG
      Resource resource = resourceLoader.getResource("classpath:jpackage/source-icon.png");
      if (!resource.exists()) {
        resource = resourceLoader.getResource("classpath:jpackage/linux/icon.png");
      }
      return ImageIO.read(resource.getInputStream());
    } catch (IOException e) {
      log.error("Failed to load application icon", e);
      return null;
    }
  }

  private void setupDockIcon(Image image) {
    if (Taskbar.isTaskbarSupported()) {
      Taskbar taskbar = Taskbar.getTaskbar();
      if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        taskbar.setIconImage(image);
        log.info("Dock/Taskbar icon set.");
      }
    }
  }

  private void launchBrowser(String url) {
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
      try {
        log.info("Launching browser to {}", url);
        Desktop.getDesktop().browse(new URI(url));
      } catch (IOException | URISyntaxException e) {
        log.error("Failed to launch browser", e);
      }
    } else {
      log.warn("Desktop browsing not supported on this platform.");
    }
  }

  private void setupSystemTray(String url, Image image) {
    if (!SystemTray.isSupported()) {
      log.warn("System Tray not supported on this platform.");
      return;
    }

    try {
      SystemTray tray = SystemTray.getSystemTray();

      PopupMenu popup = new PopupMenu();
      MenuItem openItem = new MenuItem("Open Game");
      openItem.addActionListener(e -> launchBrowser(url));
      popup.add(openItem);

      popup.addSeparator();

      MenuItem exitItem = new MenuItem("Exit");
      exitItem.addActionListener(e -> System.exit(0));
      popup.add(exitItem);

      TrayIcon trayIcon = new TrayIcon(image, "All Time Wrestling RPG", popup);
      trayIcon.setImageAutoSize(true);
      trayIcon.addActionListener(e -> launchBrowser(url));

      tray.add(trayIcon);
      log.info("System Tray icon added.");

    } catch (AWTException e) {
      log.error("Failed to setup System Tray", e);
    }
  }
}
