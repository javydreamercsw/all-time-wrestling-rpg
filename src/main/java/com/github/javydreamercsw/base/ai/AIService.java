package com.github.javydreamercsw.base.ai;

import java.util.List;
import java.util.Map;

/**
 * Abstract interface for AI services that can analyze wrestling data and generate content. This
 * interface is designed to be provider-agnostic, supporting OpenAI, Notion AI, or other providers.
 */
public interface AIService {

  /**
   * Analyzes wrestler data and provides insights.
   *
   * @param wrestlerData Map containing wrestler information
   * @return AI-generated analysis and insights
   */
  String analyzeWrestler(Map<String, Object> wrestlerData);

  /**
   * Generates match predictions based on wrestler data.
   *
   * @param wrestler1Data First wrestler's data
   * @param wrestler2Data Second wrestler's data
   * @return Predicted match outcome and analysis
   */
  String predictMatch(Map<String, Object> wrestler1Data, Map<String, Object> wrestler2Data);

  /**
   * Analyzes a wrestling show and provides insights.
   *
   * @param showData Map containing show information
   * @param matchesData List of matches in the show
   * @return AI-generated show analysis
   */
  String analyzeShow(Map<String, Object> showData, List<Map<String, Object>> matchesData);

  /**
   * Generates storyline suggestions based on wrestler heat data.
   *
   * @param heatData Map containing heat/storyline information
   * @return AI-generated storyline suggestions
   */
  String generateStorylineSuggestions(Map<String, Object> heatData);

  /**
   * Analyzes faction dynamics and provides insights.
   *
   * @param factionData Map containing faction information
   * @param membersData List of faction members' data
   * @return AI-generated faction analysis
   */
  String analyzeFaction(Map<String, Object> factionData, List<Map<String, Object>> membersData);

  /**
   * Generates creative content (promos, storylines, etc.) based on context.
   *
   * @param prompt The creative prompt
   * @param context Additional context data
   * @return AI-generated creative content
   */
  String generateCreativeContent(String prompt, Map<String, Object> context);

  /**
   * Summarizes wrestling data in a human-readable format.
   *
   * @param data The data to summarize
   * @param summaryType Type of summary (brief, detailed, analytical)
   * @return AI-generated summary
   */
  String summarizeData(Map<String, Object> data, SummaryType summaryType);

  /**
   * Gets the name/type of this AI service provider.
   *
   * @return Provider name (e.g., "OpenAI", "Notion AI", "Claude")
   */
  String getProviderName();

  /**
   * Checks if the AI service is available and properly configured.
   *
   * @return true if service is ready to use
   */
  boolean isAvailable();

  /** Types of summaries that can be generated. */
  enum SummaryType {
    BRIEF, // Short, key points only
    DETAILED, // Comprehensive overview
    ANALYTICAL // Deep analysis with insights
  }
}
