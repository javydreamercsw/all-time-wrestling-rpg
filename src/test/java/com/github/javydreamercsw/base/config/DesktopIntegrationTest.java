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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

class DesktopIntegrationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DesktopIntegration.class));

  @Test
  void testComponentDisabledByDefault() {
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(DesktopIntegration.class);
        });
  }

  @Test
  void testComponentEnabledWithProperty() {
    contextRunner
        .withPropertyValues("atw.desktop.enabled=true")
        .withBean(ResourceLoader.class, () -> mock(ResourceLoader.class))
        .run(
            context -> {
              assertThat(context).hasSingleBean(DesktopIntegration.class);
            });
  }

  @Test
  void testHeadlessEnvironmentHandling() {
    // This test ensures that even if enabled, it doesn't crash in a headless environment (standard
    // for CI)
    contextRunner
        .withPropertyValues("atw.desktop.enabled=true")
        .withBean(
            ResourceLoader.class,
            () -> {
              ResourceLoader loader = mock(ResourceLoader.class);
              Resource resource = mock(Resource.class);
              try {
                when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
              } catch (IOException e) {
                // Ignore
              }
              when(loader.getResource(anyString())).thenReturn(resource);
              return loader;
            })
        .run(
            context -> {
              DesktopIntegration integration = context.getBean(DesktopIntegration.class);
              ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

              // This should run without throwing any AWT related HeadlessExceptions
              // because of the internal checks.
              integration.onApplicationEvent(event);

              assertThat(integration).isNotNull();
            });
  }
}
