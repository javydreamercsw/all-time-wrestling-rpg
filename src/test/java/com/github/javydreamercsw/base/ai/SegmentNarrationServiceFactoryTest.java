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
package com.github.javydreamercsw.base.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationService.SegmentNarrationContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SegmentNarrationServiceFactoryTest {

  private SegmentNarrationService service1;
  private SegmentNarrationService service2;
  private SegmentNarrationContext context;

  @BeforeEach
  void setUp() {
    service1 = mock(SegmentNarrationService.class);
    service2 = mock(SegmentNarrationService.class);
    context = new SegmentNarrationContext();

    when(service1.getProviderName()).thenReturn("Service1");
    when(service2.getProviderName()).thenReturn("Service2");
    when(service1.isAvailable()).thenReturn(true);
    when(service2.isAvailable()).thenReturn(true);
  }

  @Test
  void testNarrateSegment_FallbackSuccess() {
    // Arrange
    when(service1.narrateSegment(context))
        .thenThrow(
            new AIServiceException(503, "Service Unavailable", "Service1", "Service1 is down"));
    when(service2.narrateSegment(context)).thenReturn("Successful narration from Service2");

    List<SegmentNarrationService> services = Arrays.asList(service1, service2);
    SegmentNarrationServiceFactory factory = new SegmentNarrationServiceFactory(services);

    // Act
    String result = factory.narrateSegment(context);

    // Assert
    assertEquals("Successful narration from Service2", result);
    Mockito.verify(service1, Mockito.times(1)).narrateSegment(context);
    Mockito.verify(service2, Mockito.times(1)).narrateSegment(context);
  }

  @Test
  void testNarrateSegment_AllProvidersFail() {
    // Arrange
    when(service1.narrateSegment(context))
        .thenThrow(
            new AIServiceException(503, "Service Unavailable", "Service1", "Service1 is down"));
    when(service2.narrateSegment(context))
        .thenThrow(
            new AIServiceException(
                503, "Service Unavailable", "Service2", "Service2 is also down"));

    List<SegmentNarrationService> services = Arrays.asList(service1, service2);
    SegmentNarrationServiceFactory factory = new SegmentNarrationServiceFactory(services);

    // Act & Assert
    AIServiceException exception =
        assertThrows(AIServiceException.class, () -> factory.narrateSegment(context));
    assertEquals("All AI providers failed to narrate the segment.", exception.getMessage());
    Mockito.verify(service1, Mockito.times(1)).narrateSegment(context);
    Mockito.verify(service2, Mockito.times(1)).narrateSegment(context);
  }

  @Test
  void testSummarizeNarration_FallbackSuccess() {
    // Arrange
    String narration = "This is a test narration.";
    when(service1.summarizeNarration(narration))
        .thenThrow(
            new AIServiceException(503, "Service Unavailable", "Service1", "Service1 is down"));
    when(service2.summarizeNarration(narration)).thenReturn("Successful summary from Service2");

    List<SegmentNarrationService> services = Arrays.asList(service1, service2);
    SegmentNarrationServiceFactory factory = new SegmentNarrationServiceFactory(services);

    // Act
    String result = factory.summarizeNarration(narration);

    // Assert
    assertEquals("Successful summary from Service2", result);
    Mockito.verify(service1, Mockito.times(1)).summarizeNarration(narration);
    Mockito.verify(service2, Mockito.times(1)).summarizeNarration(narration);
  }

  @Test
  void testSummarizeNarration_AllProvidersFail() {
    // Arrange
    String narration = "This is a test narration.";
    when(service1.summarizeNarration(narration))
        .thenThrow(
            new AIServiceException(503, "Service Unavailable", "Service1", "Service1 is down"));
    when(service2.summarizeNarration(narration))
        .thenThrow(
            new AIServiceException(
                503, "Service Unavailable", "Service2", "Service2 is also down"));

    List<SegmentNarrationService> services = Arrays.asList(service1, service2);
    SegmentNarrationServiceFactory factory = new SegmentNarrationServiceFactory(services);

    // Act & Assert
    AIServiceException exception =
        assertThrows(AIServiceException.class, () -> factory.summarizeNarration(narration));
    assertEquals("All AI providers failed to summarize the narration.", exception.getMessage());
    Mockito.verify(service1, Mockito.times(1)).summarizeNarration(narration);
    Mockito.verify(service2, Mockito.times(1)).summarizeNarration(narration);
  }

  @Test
  void testNarrateSegment_NoServicesAvailable() {
    // Arrange
    SegmentNarrationServiceFactory factory =
        new SegmentNarrationServiceFactory(Collections.emptyList());

    // Act & Assert
    AIServiceException exception =
        assertThrows(AIServiceException.class, () -> factory.narrateSegment(context));
    assertEquals("All AI providers failed to narrate the segment.", exception.getMessage());
  }

  @Test
  void testGenerateText_FallbackSuccess() {
    // Arrange
    String prompt = "Test prompt";
    when(service1.generateText(prompt))
        .thenThrow(
            new AIServiceException(503, "Service Unavailable", "Service1", "Service1 is down"));
    when(service2.generateText(prompt)).thenReturn("Successful text from Service2");

    List<SegmentNarrationService> services = Arrays.asList(service1, service2);
    SegmentNarrationServiceFactory factory = new SegmentNarrationServiceFactory(services);

    // Act
    String result = factory.generateText(prompt);

    // Assert
    assertEquals("Successful text from Service2", result);
    Mockito.verify(service1, Mockito.times(1)).generateText(prompt);
    Mockito.verify(service2, Mockito.times(1)).generateText(prompt);
  }

  @Test
  void testGenerateText_AllProvidersFail() {
    // Arrange
    String prompt = "Test prompt";
    when(service1.generateText(prompt))
        .thenThrow(
            new AIServiceException(503, "Service Unavailable", "Service1", "Service1 is down"));
    when(service2.generateText(prompt))
        .thenThrow(
            new AIServiceException(
                503, "Service Unavailable", "Service2", "Service2 is also down"));

    List<SegmentNarrationService> services = Arrays.asList(service1, service2);
    SegmentNarrationServiceFactory factory = new SegmentNarrationServiceFactory(services);

    // Act & Assert
    AIServiceException exception =
        assertThrows(AIServiceException.class, () -> factory.generateText(prompt));
    assertEquals("All AI providers failed to generate text.", exception.getMessage());
    Mockito.verify(service1, Mockito.times(1)).generateText(prompt);
    Mockito.verify(service2, Mockito.times(1)).generateText(prompt);
  }
}
