package com.github.javydreamercsw.base.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for MatchNarrationServiceFactory. Tests provider selection logic, cost calculations,
 * and service management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Match Narration Service Factory Tests")
class MatchNarrationServiceFactoryTest {

  @Mock private SegmentNarrationService geminiService;
  @Mock private SegmentNarrationService claudeService;
  @Mock private SegmentNarrationService openaiService;
  @Mock private SegmentNarrationService mockService;

  private SegmentNarrationServiceFactory factory;

  @BeforeEach
  void setUp() {
    // Setup mock services with lenient stubbing to avoid unnecessary stubbing warnings
    lenient().when(geminiService.getProviderName()).thenReturn("Google Gemini");
    lenient().when(claudeService.getProviderName()).thenReturn("Anthropic Claude");
    lenient().when(openaiService.getProviderName()).thenReturn("OpenAI GPT-3.5");
    lenient().when(mockService.getProviderName()).thenReturn("Mock AI");

    List<SegmentNarrationService> services =
        Arrays.asList(geminiService, claudeService, openaiService, mockService);

    factory = new SegmentNarrationServiceFactory(services);
  }

  @Test
  @DisplayName("Should select Gemini when available (highest priority)")
  void shouldSelectGeminiWhenAvailable() {
    // Given
    lenient().when(geminiService.isAvailable()).thenReturn(true);
    lenient().when(claudeService.isAvailable()).thenReturn(true);
    lenient().when(openaiService.isAvailable()).thenReturn(true);
    lenient().when(mockService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService selected = factory.getBestAvailableService();

    // Then
    assertThat(selected).isEqualTo(geminiService);
  }

  @Test
  @DisplayName("Should fallback to Claude when Gemini unavailable")
  void shouldFallbackToClaudeWhenGeminiUnavailable() {
    // Given
    lenient().when(geminiService.isAvailable()).thenReturn(false);
    lenient().when(claudeService.isAvailable()).thenReturn(true);
    lenient().when(openaiService.isAvailable()).thenReturn(true);
    lenient().when(mockService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService selected = factory.getBestAvailableService();

    // Then
    assertThat(selected).isEqualTo(claudeService);
  }

  @Test
  @DisplayName("Should fallback to OpenAI when Gemini and Claude unavailable")
  void shouldFallbackToOpenAIWhenGeminiAndClaudeUnavailable() {
    // Given
    lenient().when(geminiService.isAvailable()).thenReturn(false);
    lenient().when(claudeService.isAvailable()).thenReturn(false);
    lenient().when(openaiService.isAvailable()).thenReturn(true);
    lenient().when(mockService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService selected = factory.getBestAvailableService();

    // Then
    assertThat(selected).isEqualTo(openaiService);
  }

  @Test
  @DisplayName("Should fallback to Mock when all paid services unavailable")
  void shouldFallbackToMockWhenAllPaidServicesUnavailable() {
    // Given
    lenient().when(geminiService.isAvailable()).thenReturn(false);
    lenient().when(claudeService.isAvailable()).thenReturn(false);
    lenient().when(openaiService.isAvailable()).thenReturn(false);
    lenient().when(mockService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService selected = factory.getBestAvailableService();

    // Then
    assertThat(selected).isEqualTo(mockService);
  }

  @Test
  @DisplayName("Should return null when no services available")
  void shouldReturnNullWhenNoServicesAvailable() {
    // Given
    lenient().when(geminiService.isAvailable()).thenReturn(false);
    lenient().when(claudeService.isAvailable()).thenReturn(false);
    lenient().when(openaiService.isAvailable()).thenReturn(false);
    lenient().when(mockService.isAvailable()).thenReturn(false);

    // When
    SegmentNarrationService selected = factory.getBestAvailableService();

    // Then
    assertThat(selected).isNull();
  }

  @Test
  @DisplayName("Should find service by provider name")
  void shouldFindServiceByProviderName() {
    // Given
    lenient().when(mockService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService found = factory.getServiceByProvider("Mock");

    // Then
    assertThat(found).isEqualTo(mockService);
  }

  @Test
  @DisplayName("Should return null for unavailable provider")
  void shouldReturnNullForUnavailableProvider() {
    // Given
    lenient().when(claudeService.isAvailable()).thenReturn(false);

    // When
    SegmentNarrationService found = factory.getServiceByProvider("Claude");

    // Then
    assertThat(found).isNull();
  }

  @Test
  @DisplayName("Should return mock service for testing")
  void shouldReturnMockServiceForTesting() {
    // Given
    lenient().when(mockService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService testingService = factory.getTestingService();

    // Then
    assertThat(testingService).isEqualTo(mockService);
  }

  @Test
  @DisplayName("Should fallback to best available when mock unavailable for testing")
  void shouldFallbackToBestAvailableWhenMockUnavailableForTesting() {
    // Given
    lenient().when(mockService.isAvailable()).thenReturn(false);
    lenient().when(geminiService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService testingService = factory.getTestingService();

    // Then
    assertThat(testingService).isEqualTo(geminiService);
  }

  @Test
  @DisplayName("Should provide available services information")
  void shouldProvideAvailableServicesInformation() {
    // Given
    lenient().when(geminiService.isAvailable()).thenReturn(true);
    lenient().when(claudeService.isAvailable()).thenReturn(false);
    lenient().when(openaiService.isAvailable()).thenReturn(true);
    lenient().when(mockService.isAvailable()).thenReturn(true);

    // When
    List<SegmentNarrationServiceFactory.ServiceInfo> services = factory.getAvailableServices();

    // Then
    assertThat(services).hasSize(4);

    // Check that service info contains expected data
    SegmentNarrationServiceFactory.ServiceInfo geminiInfo =
        services.stream()
            .filter(info -> info.providerName().equals("Google Gemini"))
            .findFirst()
            .orElse(null);

    assertThat(geminiInfo).isNotNull();
    assertThat(geminiInfo.available()).isTrue();
    assertThat(geminiInfo.priority()).isEqualTo(1);
    assertThat(geminiInfo.costPer1KTokens()).isEqualTo(0.0);
    assertThat(geminiInfo.tier()).isEqualTo("FREE");
  }

  @Test
  @DisplayName("Should calculate estimated segment costs correctly")
  void shouldCalculateEstimatedSegmentCostsCorrectly() {
    // Test cost calculations for different providers

    // Gemini (free)
    double geminiCost = factory.getEstimatedSegmentCost("Google Gemini");
    assertThat(geminiCost).isEqualTo(0.0);

    // Claude
    double claudeCost = factory.getEstimatedSegmentCost("Anthropic Claude");
    assertThat(claudeCost).isGreaterThan(0.0).isLessThan(10.0);

    // OpenAI GPT-3.5
    double openaiCost = factory.getEstimatedSegmentCost("OpenAI GPT-3.5");
    assertThat(openaiCost).isGreaterThan(claudeCost).isLessThan(20.0);

    // OpenAI GPT-4
    double gpt4Cost = factory.getEstimatedSegmentCost("OpenAI GPT-4");
    assertThat(gpt4Cost).isGreaterThan(openaiCost);

    // Mock (free)
    double mockCost = factory.getEstimatedSegmentCost("Mock AI");
    assertThat(mockCost).isEqualTo(0.0);
  }

  @Test
  @DisplayName("Should handle case-insensitive provider name matching")
  void shouldHandleCaseInsensitiveProviderNameMatching() {
    // Given
    lenient().when(mockService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService found1 = factory.getServiceByProvider("mock");
    SegmentNarrationService found2 = factory.getServiceByProvider("MOCK");
    SegmentNarrationService found3 = factory.getServiceByProvider("Mock");

    // Then
    assertThat(found1).isEqualTo(mockService);
    assertThat(found2).isEqualTo(mockService);
    assertThat(found3).isEqualTo(mockService);
  }

  @Test
  @DisplayName("Should handle partial provider name matching")
  void shouldHandlePartialProviderNameMatching() {
    // Given
    lenient().when(openaiService.isAvailable()).thenReturn(true);

    // When
    SegmentNarrationService found = factory.getServiceByProvider("OpenAI");

    // Then
    assertThat(found).isEqualTo(openaiService);
  }
}
